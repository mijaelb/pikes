package eu.fbk.dkm.pikes.tintop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import eu.fbk.dkm.pikes.resources.*;
import eu.fbk.dkm.pikes.resources.ontonotes.VerbNetStatisticsExtractor;
import eu.fbk.dkm.pikes.tintop.annotators.AnnotatorUtils;
import eu.fbk.dkm.pikes.tintop.annotators.Defaults;
import eu.fbk.dkm.pikes.tintop.annotators.PikesAnnotations;
import eu.fbk.dkm.pikes.tintop.annotators.raw.Semafor;
import eu.fbk.dkm.pikes.tintop.util.NER2SSTtagset;
import eu.fbk.dkm.pikes.tintop.util.NerEntity;
import eu.fbk.dkm.pikes.twm.LinkingTag;
import eu.fbk.dkm.pikes.twm.TWMAnnotations;
import eu.fbk.utils.core.PropertiesUtils;
import ixa.kaflib.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import se.lth.cs.srl.corpus.Word;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 12:48
 * To change this template use File | Settings | File Templates.
 */

public class AnnotationPipeline {

    static Logger logger = Logger.getLogger(AnnotationPipeline.class.getName());

    enum Models {ONTONOTES, WORDNET, PREDICATE_MATRIX}

    HashMap<Models, Boolean> modelsLoaded = new HashMap<>();

    private PredicateMatrix PM;
    private VerbNetStatisticsExtractor statisticsExtractor = null;

    private Properties defaultConfig = new Properties();

    private Map<String, String> nerMap = new HashMap<>();

    public AnnotationPipeline(@Nullable File configFile, @Nullable Properties additionalProperties) throws IOException {
        defaultConfig = new Properties();
        if (configFile != null) {
            InputStream input = new FileInputStream(configFile);
            defaultConfig.load(input);
            input.close();
        }
        defaultConfig.putAll(Defaults.classProperties());
        if (additionalProperties != null) {
            defaultConfig.putAll(additionalProperties);
        }
        Defaults.setNotPresent(defaultConfig);

        for (Models model : Models.values()) {
            modelsLoaded.put(model, false);
        }
    }

    public void addToNerMap(String key, String value) {
        nerMap.put(key, value);
    }

    public void deleteFromNerMap(String key) {
        nerMap.remove(key);
    }

    public Properties getDefaultConfig() {
        return defaultConfig;
    }

    public static void addHeads(Tree node) {
        addHeads(node, null, null);
    }

    public static void addHeads(Tree node, Tree parent, HeadFinder headFinder) {
        if (node == null || node.isLeaf()) {
            return;
        }

        if (headFinder == null) {
            headFinder = new CollinsHeadFinder();
        }

        Tree head = headFinder.determineHead(node, parent);
        if (!head.isLeaf()) {
            head.label().setValue(head.label().toString() + ixa.kaflib.Tree.HEAD_MARK);
        }

        for (Tree child : node.children()) {
            addHeads(child, node, headFinder);
        }

    }

    public void loadModels() throws Exception {
        loadModels(getDefaultConfig());
    }

    public void loadModels(Properties properties) throws Exception {

        boolean enablePM = Defaults.getBoolean(properties.getProperty("enable_predicate_matrix"), false);
        boolean enableNafFilter = Defaults.getBoolean(properties.getProperty("enable_naf_filter"), false);
        boolean enableOntoNotesFilter = Defaults.getBoolean(properties.getProperty("enable_on_filter"), false);

        logger.info("Loading Stanford CoreNLP");

        Properties stanfordFromConfig = PropertiesUtils.dotConvertedProperties(properties, "stanford");
        StanfordCoreNLP stanfordPipeline = new StanfordCoreNLP(stanfordFromConfig);

        // Predicate Matrix

        if (enablePM && !modelsLoaded.get(Models.PREDICATE_MATRIX)) {
            logger.info("Loading Predicate Matrix");
            PM = new PredicateMatrix(properties.getProperty("predicate_matrix", Defaults.PREDICATE_MATRIX));
            modelsLoaded.put(Models.PREDICATE_MATRIX, true);
        }

        // NAF filter

        if (enableNafFilter && !modelsLoaded.get(Models.WORDNET)) {
            logger.info("Loading WordNet for NAF filter");
            WordNet.setPath(properties.getProperty("naf_filter_wordnet_path", Defaults.WN_DICT));
            WordNet.init();
            modelsLoaded.put(Models.WORDNET, true);
        }

        // OntoNotes

        if (enableOntoNotesFilter && !modelsLoaded.get(Models.ONTONOTES)) {
            logger.info("Loading OntoNotes");
            statisticsExtractor = new VerbNetStatisticsExtractor();
//			statisticsExtractor.loadDir(config.getProperty("on_folder"));
//			statisticsExtractor.loadFrequencies();
            statisticsExtractor.loadFrequencies(properties.getProperty("on_frequencies", Defaults.ON_FREQUENCIES));
            modelsLoaded.put(Models.ONTONOTES, true);
        }
    }

    public void annotateStanford(Properties properties, Annotation document, KAFDocument NAFdocument)
            throws IOException {

        boolean enablePM = Defaults.getBoolean(properties.getProperty("enable_predicate_matrix"), false);
        boolean enableNafFilter = Defaults.getBoolean(properties.getProperty("enable_naf_filter"), false);
        boolean enableOntoNotesFilter = Defaults.getBoolean(properties.getProperty("enable_on_filter"), false);
        boolean enableEntityAssignment = Defaults.getBoolean(properties.getProperty("enable_entity_assignment"), false);

        Map<Integer, CorefChain> coreferenceGraph = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        // Add tmx0
        try {
            Timex3 tmx0 = NAFdocument.newTimex3("tmx0", "DATE");
            tmx0.setValue(NAFdocument.getFileDesc().creationtime.substring(0, 10));
        } catch (Exception e) {
            logger.warn("Document creation time is not included in the NAF headers");
        }

        logger.info("Getting information");
        TreeMap<Integer, Integer> sentIndexes = new TreeMap<>();
        int totTokens = 0;
        ArrayList<Term> allTerms = new ArrayList<>();

        HashMap<Integer, Integer> tokenFromStart = new HashMap<>();
        HashMap<Integer, Integer> tokenFromEnd = new HashMap<>();

        ArrayList<WF> allTokens = new ArrayList<>();
        HashMap<Integer, HashSet<LinkingTag>> keywords = new HashMap<>();

        if (document.has(TWMAnnotations.LinkingAnnotations.class)) {
            for (LinkingTag e : document.get(TWMAnnotations.LinkingAnnotations.class)) {
                int start = e.getOffset();
                if (keywords.get(start) == null) {
                    keywords.put(start, new HashSet<LinkingTag>());
                }
                keywords.get(start).add(e);
                logger.debug("Annotated entity (DS): " + e);
            }
        }

        // Main loop
        List<CoreMap> get = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (int sentIndex = 0; sentIndex < get.size(); sentIndex++) {
            CoreMap stanfordSentence = get.get(sentIndex);
            List<CoreLabel> tokens = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);

            ArrayList<Term> terms = new ArrayList<>();
            ArrayList<String> ners = new ArrayList<>();

            sentIndexes.put(sentIndex, totTokens);
            totTokens += tokens.size();

            HashMap<Integer, TreeSet<Integer>> children = new HashMap<>();

            String lastNER = "O";
            ArrayList<NerEntity> entities = new ArrayList<>();

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel stanfordToken = tokens.get(i);
                String form = stanfordToken.get(CoreAnnotations.TextAnnotation.class);
                String lemma = stanfordToken.get(CoreAnnotations.LemmaAnnotation.class);
                String pos = stanfordToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);

                form = AnnotatorUtils.codeToParenthesis(form);
                if (lemma != null) {
                    lemma = AnnotatorUtils.codeToParenthesis(lemma);
                }
                pos = AnnotatorUtils.codeToParenthesis(pos);

                children.put(i, new TreeSet<Integer>());

                // Tokens
                WF thisWF = NAFdocument.newWF(form, stanfordToken.beginPosition(), sentIndex + 1);
                thisWF.setPara(1); //todo: Always set paragraph 1

                Integer tokenID = totTokens - tokens.size() + i;

                tokenFromStart.put(stanfordToken.beginPosition(), tokenID);
                tokenFromEnd.put(stanfordToken.beginPosition() + thisWF.getLength(), tokenID);
                allTokens.add(tokenID, thisWF);

                // Term
                Span<WF> thisWFSpan = KAFDocument.newWFSpan();
                thisWFSpan.addTarget(thisWF);
                Term thisTerm = NAFdocument.newTerm("open", lemma, pos, thisWFSpan);
                thisTerm.setMorphofeat(pos);

                // WordNet sense
                String wnSense = stanfordToken.get(PikesAnnotations.UKBAnnotation.class);
                if (wnSense != null) {
                    thisTerm.setWordnetSense(stanfordToken.get(PikesAnnotations.UKBAnnotation.class));
                }

                // Simple POS
                String simplePos = stanfordToken.get(PikesAnnotations.SimplePosAnnotation.class);
                if (simplePos == null) {
                    simplePos = "O";
                }
                thisTerm.setPos(simplePos);

                terms.add(thisTerm);
                allTerms.add(thisTerm);

                String ne = stanfordToken.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                if (nerMap.containsKey(ne)) {
                    ne = nerMap.get(ne);
                }
                String normVal = stanfordToken.getString(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class);
                if (ne != null) {
                    if (ne.equals("O")) {
                        ners.add("0");
                    } else {

                        // Alternative string for SST
                        String alt = NER2SSTtagset.tagset.get(ne);
                        if (alt == null) {
                            alt = "MISC";
                        }

                        if (ne.equals(lastNER)) {
                            entities.get(entities.size() - 1).setEndToken(i);
                            ners.add("I-" + alt);
                        } else {
                            NerEntity newEntity = new NerEntity(ne, i, normVal);
                            entities.add(newEntity);
                            ners.add("B-" + alt);
                        }
                    }
                    lastNER = ne;
                } else {
                    ners.add("0");
                }

            }

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel stanfordToken = tokens.get(i);

                // Dependencies
                if (!stanfordToken.has(CoreAnnotations.CoNLLDepParentIndexAnnotation.class)) {
                    continue;
                }

                int head = stanfordToken.get(CoreAnnotations.CoNLLDepParentIndexAnnotation.class);
                head++;
                String depRel = stanfordToken.get(CoreAnnotations.CoNLLDepTypeAnnotation.class);
                if (head != 0) {
                    Term from = terms.get(head - 1);
                    Term to = terms.get(i);
                    NAFdocument.newDep(from, to, depRel);
                }

                Word word = stanfordToken.get(PikesAnnotations.MateTokenAnnotation.class);
                if (word != null) {
                    List<Word> toRoot = Word.pathToRoot(word);
                    for (Word w : toRoot) {
                        int id = w.getIdx() - 1;
                        if (id < 0) {
                            continue;
                        }
                        children.get(id).add(i);
                    }
                }
            }

            // Opinion

//            boolean includeNeutral = config.getProperty("stanford_include_neutral", "0").equals("1");
//
//            Tree sentimentTree = stanfordSentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
//            if (sentimentTree != null) {
//                HashMap<edu.stanford.nlp.ling.Word, Term> indexedWords = new HashMap<>();
//                int wordIndex = -1;
//                for (Tree t : sentimentTree.getLeaves()) {
//                    wordIndex++;
//                    List<edu.stanford.nlp.ling.Word> words = t.yieldWords();
//                    for (edu.stanford.nlp.ling.Word w : words) {
//                        indexedWords.put(w, terms.get(wordIndex));
//                    }
//                }
//
//                for (Tree tree : sentimentTree) {
//
//                    Integer predictedClass;
//                    try {
//                        predictedClass = RNNCoreAnnotations.getPredictedClass(tree);
//                    } catch (Exception e) {
//                        continue;
//                    }
//
//                    if (predictedClass == null) {
//                        continue;
//                    }
//
//                    if (!includeNeutral && predictedClass == 2) {
//                        continue;
//                    }
//
//                    Span<Term> treeSpan = KAFDocument.newTermSpan();
//                    for (edu.stanford.nlp.ling.Word word : tree.yieldWords()) {
//                        treeSpan.addTarget(indexedWords.get(word));
//                    }
//
//                    Opinion opinion = NAFdocument.createOpinion();
//                    opinion.setLabel("stanford-sentiment");
//                    Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(treeSpan);
//                    opinionExpression.setPolarity(stanfordSentimentLabels[predictedClass]);
//
//                    NumberFormat nf = NumberFormat.getNumberInstance();
//                    nf.setMaximumFractionDigits(2);
//
//                    SimpleMatrix predictions = RNNCoreAnnotations.getPredictions(tree);
//                    StringBuffer stringBuffer = new StringBuffer();
//                    stringBuffer.append(nf.format(predictions.get(0)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(1)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(2)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(3)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(4)));
//                    opinionExpression.setStrength(stringBuffer.toString());
//                }
//            }

            // Entities

            for (NerEntity entity : entities) {

                int startIndex = terms.get(entity.getStartToken()).getWFs().get(0).getOffset();
                int endIndex = terms.get(entity.getEndToken()).getWFs()
                        .get(terms.get(entity.getEndToken()).getWFs().size() - 1).getOffset() +
                        terms.get(entity.getEndToken()).getWFs()
                                .get(terms.get(entity.getEndToken()).getWFs().size() - 1).getLength();

                logger.debug("Stanford NER entity: " + entity + "");
                logger.debug(String.format("Stanford NER entity: %s (from %d to %d)", entity.getLabel(), startIndex,
                        endIndex));

                Span<Term> thisTermSpan = KAFDocument.newTermSpan();
                Span<WF> thisWFSpan = KAFDocument.newWFSpan();

                for (int i = entity.getStartToken(); i <= entity.getEndToken(); i++) {
                    thisTermSpan.addTarget(terms.get(i));
                    thisWFSpan.addTargets(terms.get(i).getWFs());
                }

                List<Span<Term>> thisTermList = new LinkedList<>();
                List<Span<WF>> thisWFList = new LinkedList<>();

                thisTermList.add(thisTermSpan);
                thisWFList.add(thisWFSpan);

                Entity thisEntity;
                Timex3 thisTimex;

                switch (entity.getLabel()) {
                case "PERSON":
                case "LOCATION":
                case "ORGANIZATION":
                case "MISC":
                case "MONEY":
                case "PERCENT":
                    thisEntity = NAFdocument.newEntity(thisTermList);
                    thisEntity.setType(entity.getLabel());

                    // Normalized value
                    if (entity.getNormalizedValue() != null && entity.getNormalizedValue().length() > 0) {
                        thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));
                    }

                    if (enableEntityAssignment) {
                        LinkingTag e = null;
                        HashSet<LinkingTag> possibleEntities = keywords.get(startIndex);
                        if (possibleEntities != null) {
                            for (LinkingTag loopEntity : possibleEntities) {
                                int end = loopEntity.getOffset() + loopEntity.getLength();
                                if (end != endIndex) {
                                    continue;
                                }
                                if (e == null || e.getScore() < loopEntity.getScore()) {
                                    e = loopEntity;
                                }
                            }
                        }

                        if (e != null) {
                            ExternalRef ext = NAFdocument.newExternalRef(e.getSource(), e.getPage());
                            ext.setConfidence((float) e.getScore());
                            thisEntity.addExternalRef(ext);
                        }
                    }

                    break;

                case "NUMBER":
                    thisEntity = NAFdocument.newEntity(thisTermList);
                    thisEntity.setType("CARDINAL");
                    thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));
                    break;

                case "ORDINAL":
                    thisEntity = NAFdocument.newEntity(thisTermList);
                    thisEntity.setType("ORDINAL");
                    thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));
                    break;

                case "DATE":
                case "TIME":
                    thisTimex = NAFdocument.newTimex3(thisWFSpan, entity.getLabel());
                    thisTimex.setValue(entity.getNormalizedValue());
                    break;

                case "DURATION":
                    thisTimex = NAFdocument.newTimex3(thisWFSpan, entity.getLabel());
                    thisTimex.setValue(entity.getNormalizedValue());
                    break;

                default:
                    logger.debug(entity.getLabel());
                }
            }

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel stanfordToken = tokens.get(i);

                se.lth.cs.srl.corpus.Predicate predicate = stanfordToken.get(PikesAnnotations.MateAnnotation.class);
                if (predicate != null) {
                    Span<Term> thisTermSpan = KAFDocument.newTermSpan();
                    Term thisTerm = terms.get(predicate.getIdx() - 1);
                    String tmpSense = predicate.getSense();

                    thisTermSpan.addTarget(thisTerm);

                    Predicate newPred = NAFdocument.newPredicate(thisTermSpan);
                    newPred.setSource("mate");

                    ExternalRef e;
                    // If it's a verb -> PropBank, if it's a noun -> NomBank
                    if (thisTerm.getPos().equals("V")) {
                        e = NAFdocument.newExternalRef("PropBank", tmpSense);
                        e.setSource("mate");
                    } else {
                        e = NAFdocument.newExternalRef("NomBank", tmpSense);
                        e.setSource("mate");
                    }
                    newPred.addExternalRef(e);

                    String sense = null;
                    if (thisTerm.getPos().equals("V")) {
                        sense = tmpSense;
                    } else {
                        // check NomBank
                        NomBank.Roleset roleset = NomBank.getRoleset(tmpSense);
                        try {
                            sense = roleset.getPBId();
                        } catch (Exception ex) {
                            logger.debug(ex.getMessage());
                        }
                    }

                    ArrayList<String> vnClasses = new ArrayList<>();
                    ArrayList<String> fnFrames = new ArrayList<>();

                    if (enablePM) {
                        if (sense != null && sense.length() > 0) {

                            HashSet<String> vnToAdd = new HashSet<>();
                            String vnFinal = null;

                            // VerbNet
                            vnClasses = PM.getVNClasses(sense);
                            if (!vnClasses.isEmpty()) {
                                if (vnClasses.size() == 1 || !enableOntoNotesFilter) {
                                    for (String vnClass1 : vnClasses) {
                                        vnToAdd.add(vnClass1);
                                        vnFinal = vnClass1;
                                    }
                                } else {
                                    Integer value = 0;

                                    for (String vnClass : vnClasses) {
                                        Integer thisValue = statisticsExtractor.getVnTotals().get(vnClass);
                                        thisValue = thisValue == null ? 0 : thisValue;
                                        if (thisValue >= value) {
                                            vnFinal = vnClass;
                                            value = thisValue;
                                        }
                                    }

                                    // Reset the list of classes
                                    vnClasses = new ArrayList<>();

                                    if (vnFinal != null) {
                                        vnToAdd.add(vnFinal);
                                        vnClasses.add(vnFinal);
                                    }
                                }
                            }
                            ArrayList<String> vnSubClasses = PM.getVNSubClasses(sense);
                            if (!vnSubClasses.isEmpty()) {
                                for (String vnSubClass1 : vnSubClasses) {
                                    for (String vnClass : vnClasses) {
                                        if (!vnSubClass1.startsWith(vnClass)) {
                                            continue;
                                        }

                                        vnToAdd.add(vnSubClass1);

                                        // Remove upper class
                                        if (vnFinal != null) {
                                            if (vnSubClass1.startsWith(vnFinal)) {
                                                vnToAdd.remove(vnFinal);
                                            }
                                        }
                                    }
                                }
                            }

                            for (String vnClass1 : vnToAdd) {
                                ExternalRef vnClass = NAFdocument.newExternalRef("VerbNet", vnClass1);
                                newPred.addExternalRef(vnClass);
                            }

                            // FrameNet
                            fnFrames = PM.getFNFrames(sense);

                            if (enableOntoNotesFilter) {
                                HashSet<String> possibleFrames = new HashSet<>();
                                for (String vnClass : vnClasses) {
                                    possibleFrames.addAll(PM.getVNClassesToFN(vnClass));
                                }

//								System.out.println("vnClasses: " + vnClasses);
//								System.out.println("fnFrames (before): " + fnFrames);
                                fnFrames.retainAll(possibleFrames);
//								System.out.println("fnFrames (after): " + fnFrames);
//								System.out.println("Possible frames: " + possibleFrames);
                            }

                            if (!fnFrames.isEmpty()) {
                                if (fnFrames.size() == 1 || !enableOntoNotesFilter) {
                                    for (String fnFrame1 : fnFrames) {
                                        ExternalRef fnFrame = NAFdocument.newExternalRef("FrameNet", fnFrame1);
                                        newPred.addExternalRef(fnFrame);
                                    }
                                } else {
                                    Integer value = 0;
                                    String fnFinal = null;

                                    for (String fnFrame : fnFrames) {
                                        Integer thisValue = statisticsExtractor.getFnTotals()
                                                .get(fnFrame.toLowerCase());
                                        thisValue = thisValue == null ? 0 : thisValue;
                                        if (thisValue >= value) {
                                            fnFinal = fnFrame;
                                            value = thisValue;
                                        }
                                    }

                                    // Reset the list of frames
                                    fnFrames = new ArrayList<>();

                                    if (fnFinal != null) {
                                        ExternalRef fnFrame = NAFdocument.newExternalRef("FrameNet", fnFinal);
                                        newPred.addExternalRef(fnFrame);
                                        fnFrames.add(fnFinal);
                                    }
                                }
                            }

                            // PropBank
                            ArrayList<String> pbPredicates = PM.getPBPredicates(sense);
                            if (!pbPredicates.isEmpty()) {
                                for (String pbPredicate1 : pbPredicates) {
                                    ExternalRef pbPredicate = NAFdocument.newExternalRef("PropBank", pbPredicate1);
                                    newPred.addExternalRef(pbPredicate);
                                }
                            }

                            // ESO
                            ArrayList<String> esoClasses = PM.getESOClasses(sense);
                            if (!esoClasses.isEmpty()) {
                                for (String esoClass1 : esoClasses) {
                                    ExternalRef esoClass = NAFdocument.newExternalRef("ESO", esoClass1);
                                    newPred.addExternalRef(esoClass);
                                }
                            }

                            // EventType
                            ArrayList<String> eventTypes = PM.getEventTypes(sense);
                            if (!eventTypes.isEmpty()) {
                                for (String eventType1 : eventTypes) {
                                    ExternalRef eventType = NAFdocument.newExternalRef("EventType", eventType1);
                                    newPred.addExternalRef(eventType);
                                }
                            }

                            // WordNet
                            ArrayList<String> wnSenses = PM.getWNSenses(sense);
                            if (!wnSenses.isEmpty()) {
                                for (String wnSense1 : wnSenses) {
                                    ExternalRef wnSense = NAFdocument.newExternalRef("WordNet", wnSense1);
                                    newPred.addExternalRef(wnSense);
                                }
                            }

                        }
                    }

                    for (Word w : predicate.getArgMap().keySet()) {
                        Span<Term> thisTermSpanForRole = KAFDocument.newTermSpan();
                        for (int k : children.get(w.getIdx() - 1)) {
                            thisTermSpanForRole.addTarget(terms.get(k));
                        }
                        thisTermSpanForRole.setHead(terms.get(w.getIdx() - 1));

                        String argument = predicate.getArgMap().get(w);
                        Predicate.Role newRole = NAFdocument.newRole(newPred, argument, thisTermSpanForRole);

                        if (enablePM && PM != null && statisticsExtractor != null) {

                            // VerbNet
                            ArrayList<String> vnThematicRoles = PM.getVNThematicRoles(sense + ":" + argument);
                            if (!vnThematicRoles.isEmpty()) {
                                for (String vnThematicRole1 : vnThematicRoles) {
                                    if (!enableOntoNotesFilter) {
                                        ExternalRef vnThematicRole = NAFdocument
                                                .newExternalRef("VerbNet", vnThematicRole1);
                                        newRole.addExternalRef(vnThematicRole);
                                    } else {
                                        String[] parts = vnThematicRole1.split("@");
                                        if (vnClasses.contains(parts[0])) {
                                            ExternalRef vnThematicRole = NAFdocument
                                                    .newExternalRef("VerbNet", vnThematicRole1);
                                            newRole.addExternalRef(vnThematicRole);
                                        }
                                    }
                                }
                            }

                            // FrameNet
                            ArrayList<String> fnFrameElements = PM.getFNFrameElements(sense + ":" + argument);
                            if (!fnFrameElements.isEmpty()) {
                                for (String fnFrameElement1 : fnFrameElements) {
                                    if (!enableOntoNotesFilter) {
                                        ExternalRef fnFrameElement = NAFdocument
                                                .newExternalRef("FrameNet", fnFrameElement1);
                                        newRole.addExternalRef(fnFrameElement);
                                    } else {
                                        String[] parts = fnFrameElement1.split("@");
                                        if (fnFrames.contains(parts[0])) {
                                            ExternalRef fnFrameElement = NAFdocument
                                                    .newExternalRef("FrameNet", fnFrameElement1);
                                            newRole.addExternalRef(fnFrameElement);
                                        }
                                    }
                                }
                            }

                            // PropBank
                            ArrayList<String> pbArguments = PM.getPBArguments(sense + ":" + argument);
                            if (!pbArguments.isEmpty()) {
                                for (String pbArgument1 : pbArguments) {
                                    ExternalRef pbArgument = NAFdocument.newExternalRef("PropBank", pbArgument1);
                                    newRole.addExternalRef(pbArgument);
                                }
                            }

                            // ESO
                            ArrayList<String> esoRoles = PM.getESORoles(sense + ":" + argument);
                            if (!esoRoles.isEmpty()) {
                                for (String esoRole1 : esoRoles) {
                                    ExternalRef esoRole = NAFdocument.newExternalRef("ESO", esoRole1);
                                    newRole.addExternalRef(esoRole);
                                }
                            }
                        }

                        newPred.addRole(newRole);
                    }

                }
            }

            if (stanfordSentence.has(PikesAnnotations.SemaforAnnotation.class)) {
                SemaforParseResult semaforParseResult = stanfordSentence.get(PikesAnnotations.SemaforAnnotation.class);
                ObjectMapper mapper = new ObjectMapper();
                Semafor.SemaforResponse semaforResponse = mapper
                        .readValue(semaforParseResult.toJson(), Semafor.SemaforResponse.class);
                for (Semafor.SemaforFrame semaforFrame : semaforResponse.getFrames()) {
                    Semafor.SemaforAnnotation semaforTarget = semaforFrame.getTarget();
                    if (semaforTarget == null) {
                        continue;
                    }
                    String frameName = semaforTarget.getName();

                    if (semaforTarget.getSpans().size() == 0) {
                        continue;
                    }
                    if (semaforFrame.getAnnotationSets().size() == 0) {
                        continue;
                    }

                    Semafor.SemaforSpan semaforSpan = semaforTarget.getSpans().get(0);
                    Semafor.SemaforSet semaforAnnotation = semaforFrame.getAnnotationSets().get(0);

                    Span<Term> termSpan = KAFDocument.newTermSpan();
                    for (int i = semaforSpan.getStart(); i < semaforSpan.getEnd(); i++) {
                        termSpan.addTarget(terms.get(i));
                    }

                    if (termSpan.size() == 0) {
                        continue;
                    }

                    Predicate predicate = NAFdocument.newPredicate(termSpan);
                    predicate.setSource("semafor");
                    predicate.setConfidence(semaforAnnotation.getScore());
                    predicate.addExternalRef(NAFdocument.createExternalRef("FrameNet", frameName));
                    predicate.setId("f_" + predicate.getId());

                    for (Semafor.SemaforAnnotation frameAnnotation : semaforAnnotation.getFrameElements()) {
                        Semafor.SemaforSpan roleSpan = frameAnnotation.getSpans().get(0);
                        String roleName = frameAnnotation.getName();

                        Span<Term> roleTermSpan = KAFDocument.newTermSpan();
                        for (int i = roleSpan.getStart(); i < roleSpan.getEnd(); i++) {
                            roleTermSpan.addTarget(terms.get(i));
                        }

                        if (roleTermSpan.size() == 0) {
                            continue;
                        }

                        Predicate.Role role = NAFdocument.newRole(predicate, "", roleTermSpan);
                        final Term head = NAFUtils.extractHead(NAFdocument, role.getSpan());
                        if (head != null) {
                            final Span<Term> newSpan = KAFDocument
                                    .newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(
                                            NAFdocument.getTermsByDepAncestors(ImmutableList.of(head))));
                            role.setSpan(newSpan);
                        }
                        role.addExternalRef(
                                NAFdocument.createExternalRef("FrameNet", frameName + "@" + roleName));
                        predicate.addRole(role);
                    }

                }
            }

            // Constituency
            Tree tree = stanfordSentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            if (tree != null) {
                NAFdocument.addConstituencyString(tree.toString(), sentIndex + 1);
                try {
                    logger.debug("Tree: " + tree.toString());
                    addHeads(tree);
                    NAFdocument.addConstituencyFromParentheses(tree.toString(), sentIndex + 1);
                } catch (Exception e) {
                    logger.info("Tree: " + tree.toString());
                    logger.warn(e.getMessage());
                    e.printStackTrace();
                }
            }

        } // end sentences loop

        // Entities
        for (Integer startIndex : keywords.keySet()) {
            for (LinkingTag e : keywords.get(startIndex)) {
                int end = e.getOffset() + e.getLength();
                Integer startToken = tokenFromStart.get(e.getOffset());
                Integer endToken = tokenFromEnd.get(end);
                Span<WF> span = KAFDocument.newWFSpan();
                if (startToken != null && endToken != null) {
                    for (int j = startToken; j <= endToken; j++) {
                        span.addTarget(allTokens.get(j));
                    }

                    try {
                        LinkedEntity linkedEntity = NAFdocument.newLinkedEntity(span);
                        linkedEntity.setConfidence(e.getScore());
                        linkedEntity.setReference(e.getPage());
                        linkedEntity.setResource(e.getSource());
                        linkedEntity.setTypes(e.getStringTypes());
                        linkedEntity.setSpotted(e.isSpotted());
                    } catch (Exception err) {
                        logger.error("Error on adding linkedEntity: " + err.getMessage());
                    }
                }
            }
        }

        // Coref

        // Loop through clusters
        if (coreferenceGraph != null) {
            for (Object c : coreferenceGraph.keySet()) {

                CorefChain chain = coreferenceGraph.get(c);
                Map<IntPair, Set<CorefChain.CorefMention>> mentionMap = chain.getMentionMap();

                // Skip coreference if its size is 1
                if (mentionMap.size() < 2) {
                    continue;
                }

                List<Span<Term>> mentions = new ArrayList<>();

                // Loop through sentences
                for (IntPair p : mentionMap.keySet()) {

                    // Loop through mentions
                    for (CorefChain.CorefMention m : mentionMap.get(p)) {

                        int sentenceStartTokenIndex = sentIndexes.get(m.sentNum - 1);
                        int start = sentenceStartTokenIndex + m.startIndex - 1;

                        Span<Term> thisTermSpan = KAFDocument.newTermSpan();
                        for (int i = start; i < start + m.endIndex - m.startIndex; i++) {
                            thisTermSpan.addTarget(allTerms.get(i));
                        }
                        if (!thisTermSpan.isEmpty()) {
                            mentions.add(thisTermSpan);
                        }

//					logger.info(m.animacy);
//					logger.info(m.gender);
//					logger.info(m.mentionSpan);
//					logger.info(m.mentionType);
//					logger.info(m.number);
                    }
                }

                if (mentions.size() > 0) {
                    NAFdocument.newCoref(mentions);
                }
            }
        }

        // NAF filter
        if (enableNafFilter) {
            logger.info("Applying NAF filter");
            LinguisticProcessor linguisticProcessor = new LinguisticProcessor("naf-filter", "NAF filter");
            linguisticProcessor.setBeginTimestamp();
            try {
                NAFFilter.builder(false)
                        .withTermSenseCompletion(true).withSRLRoleLinking(false, false)
                        .withOpinionLinking(false, false).build()
                        .filter(NAFdocument);
            } catch (Exception e) {
                logger.error("Error applying NAF filter");
            }
            linguisticProcessor.setEndTimestamp();
            NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
        }
    }

    private KAFDocument parseAll(KAFDocument NAFdocument) throws Exception {
        return parseAll(NAFdocument, new Properties());
    }

    private KAFDocument parseAll(KAFDocument NAFdocument, Properties merge) throws Exception {

        String text = NAFdocument.getRawText();
        text = StringEscapeUtils.unescapeHtml(text);

        Properties properties = getDefaultConfig();
        properties.putAll(merge);

        String maxTextLen = properties.getProperty("max_text_len");
        int limit = Integer.parseInt(maxTextLen);
        if (text.length() > limit) {
            throw new Exception(String.format("Input too long (%d chars, limit is %d)", text.length(), limit));
        }

        loadModels(properties);
        Properties stanfordConfig = PropertiesUtils.dotConvertedProperties(properties, "stanford");

        // Load pipeline
        Properties thisSessionProps = new Properties(stanfordConfig);
        StanfordCoreNLP thisPipeline = new StanfordCoreNLP(thisSessionProps);

        // Stanford
        logger.info("Annotating with Stanford CoreNLP");
        LinguisticProcessor linguisticProcessor = new LinguisticProcessor("text", "Stanford CoreNLP");
        linguisticProcessor.setBeginTimestamp();
        Annotation document = new Annotation(text);
        document.set(CoreAnnotations.DocDateAnnotation.class, NAFdocument.getFileDesc().creationtime);
        thisPipeline.annotate(document);
        logger.info(thisPipeline.timingInformation());
        linguisticProcessor.setEndTimestamp();
        NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);

        annotateStanford(properties, document, NAFdocument);

        logger.info("Parsing finished");
        return NAFdocument;
    }

    public KAFDocument parseFromNAF(KAFDocument NAFdocument) throws Exception {

        NAFdocument = parseAll(NAFdocument);

        return NAFdocument;
    }

    public KAFDocument parseFromString(String textInNafFormat) throws Exception {
        logger.debug("Parsing of NAF");

        InputStream is = new ByteArrayInputStream(textInNafFormat.getBytes());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        KAFDocument NAFdocument = KAFDocument.createFromStream(br);

        try {
            logger.info("Document: " + NAFdocument.getFileDesc().filename);
            logger.info("Title: " + NAFdocument.getFileDesc().title);
//            logger.debug("Text: " + NAFdocument.getRawText());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        NAFdocument = parseAll(NAFdocument);

        return NAFdocument;
    }

}
