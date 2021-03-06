package eu.fbk.dkm.pikes.naflib;

import java.io.File;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import eu.fbk.utils.core.EasySpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ixa.kaflib.KAFDocument;

import eu.fbk.utils.core.CommandLine;

/**
 * Created by alessio on 25/03/15.
 */

public class NafOffsetReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(NafOffsetReader.class);

	public static void main(String[] args) {
		final CommandLine cmd = CommandLine
				.parser()
				.withName("naf-offset-reader")
				.withHeader("Read offset from a file")
				.withOption("s", "start", "starting offset", "NUM", CommandLine.Type.INTEGER, true, false, true)
				.withOption("e", "end", "ending offset", "NUM", CommandLine.Type.INTEGER, true, false, true)
				.withOption("n", "naf", "file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
				.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

		final Integer start = cmd.getOptionValue("s", Integer.class);
		final Integer end = cmd.getOptionValue("e", Integer.class);
		final File nafFile = cmd.getOptionValue("n", File.class);

		try {
			String text;
			try {
				KAFDocument document = KAFDocument.createFromFile(nafFile);
				text = document.getRawText();
			} catch (Exception e) {
				text = Files.toString(nafFile, Charsets.UTF_8);
			}
			EasySpan span = new EasySpan(start, end);
			String piece = span.apply(text, false);

			System.out.println(span);
			System.out.println("===" + piece + "===");
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}
}
