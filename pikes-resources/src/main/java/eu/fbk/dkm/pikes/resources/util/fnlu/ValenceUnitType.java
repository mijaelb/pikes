//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.12 at 10:43:37 PM CET 
//


package eu.fbk.dkm.pikes.resources.util.fnlu;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for valenceUnitType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="valenceUnitType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="FE" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="PT" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="GF" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "valenceUnitType")
public class ValenceUnitType {

    @XmlAttribute(name = "FE", required = true)
    protected String fe;
    @XmlAttribute(name = "PT", required = true)
    protected String pt;
    @XmlAttribute(name = "GF", required = true)
    protected String gf;

    /**
     * Gets the value of the fe property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFE() {
        return fe;
    }

    /**
     * Sets the value of the fe property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFE(String value) {
        this.fe = value;
    }

    /**
     * Gets the value of the pt property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPT() {
        return pt;
    }

    /**
     * Sets the value of the pt property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPT(String value) {
        this.pt = value;
    }

    /**
     * Gets the value of the gf property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGF() {
        return gf;
    }

    /**
     * Sets the value of the gf property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGF(String value) {
        this.gf = value;
    }

}
