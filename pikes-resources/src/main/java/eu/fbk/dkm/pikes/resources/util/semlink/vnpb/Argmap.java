//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.13 at 05:16:47 PM CET 
//


package eu.fbk.dkm.pikes.resources.util.semlink.vnpb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "role"
})
@XmlRootElement(name = "argmap")
public class Argmap {

    @XmlAttribute(name = "pb-roleset", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String pbRoleset;
    @XmlAttribute(name = "vn-class", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String vnClass;
    @XmlElement(required = true)
    protected List<Role> role;

    /**
     * Gets the value of the pbRoleset property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPbRoleset() {
        return pbRoleset;
    }

    /**
     * Sets the value of the pbRoleset property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPbRoleset(String value) {
        this.pbRoleset = value;
    }

    /**
     * Gets the value of the vnClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVnClass() {
        return vnClass;
    }

    /**
     * Sets the value of the vnClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVnClass(String value) {
        this.vnClass = value;
    }

    /**
     * Gets the value of the role property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the role property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRole().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Role }
     * 
     * 
     */
    public List<Role> getRole() {
        if (role == null) {
            role = new ArrayList<Role>();
        }
        return this.role;
    }

}