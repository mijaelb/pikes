//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.13 at 04:30:18 PM CET 
//


package eu.fbk.dkm.pikes.resources.util.semlink.vnfn;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
@XmlRootElement(name = "vncls")
public class Vncls {

    @XmlAttribute(name = "class", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String clazz;
    @XmlAttribute(name = "vnmember", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String vnmember;
    @XmlAttribute(name = "fnframe", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String fnframe;
    @XmlAttribute(name = "fnlexent", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String fnlexent;
    @XmlAttribute(name = "versionID")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String versionID;
    @XmlValue
    protected String value;

    /**
     * Gets the value of the clazz property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * Sets the value of the clazz property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClazz(String value) {
        this.clazz = value;
    }

    /**
     * Gets the value of the vnmember property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVnmember() {
        return vnmember;
    }

    /**
     * Sets the value of the vnmember property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVnmember(String value) {
        this.vnmember = value;
    }

    /**
     * Gets the value of the fnframe property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFnframe() {
        return fnframe;
    }

    /**
     * Sets the value of the fnframe property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFnframe(String value) {
        this.fnframe = value;
    }

    /**
     * Gets the value of the fnlexent property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFnlexent() {
        return fnlexent;
    }

    /**
     * Sets the value of the fnlexent property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFnlexent(String value) {
        this.fnlexent = value;
    }

    /**
     * Gets the value of the versionID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersionID() {
        if (versionID == null) {
            return "vn3.2";
        } else {
            return versionID;
        }
    }

    /**
     * Sets the value of the versionID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersionID(String value) {
        this.versionID = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getvalue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setvalue(String value) {
        this.value = value;
    }

}
