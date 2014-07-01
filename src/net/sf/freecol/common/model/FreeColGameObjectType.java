/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.option.OptionGroup;


/**
 * The base class for all types defined by the specification. It can
 * be instantiated in order to provide a source for modifiers and
 * abilities that are provided by the code rather than defined in the
 * specification, such as the "artillery in the open" penalty.
 *
 * In general, a FreeColGameObjectType does not need a reference to
 * the specification.  However, if it has attributes or children that
 * are themselves FreeColGameObjectTypes, then the specification must
 * be set before the type is de-serialized, otherwise the identifiers
 * can not be resolved.
 *
 * FreeColGameObjectTypes can be abstract.  Abstract types can be used
 * to derive other types, but can not be instantiated.  They will be
 * removed from the Specification after it has loaded completely.
 */
public abstract class FreeColGameObjectType extends FreeColObject
    implements Named {

    /** Whether the type is abstract, or can be instantiated. */
    private boolean abstractType;

    /**
     * The features of this game object type.  Feature containers are
     * created on demand.
     */
    private FeatureContainer featureContainer = null;

    // Do not serialize below.

    /**
     * The index imposes a total ordering consistent with equals on
     * each class extending FreeColGameObjectType, but this ordering
     * is nothing but the order in which the objects of the respective
     * class were defined.  It is guaranteed to remain stable only for
     * a particular revision of a particular specification.
     */
    private int index = -1;
    

    /**
     * Deliberately empty constructor.
     */
    protected FreeColGameObjectType() {}

    /**
     * Create a simple FreeColGameObjectType without a specification.
     *
     * @param id The object identifier.
     */
    public FreeColGameObjectType(String id) {
        this(id, null);
    }

    /**
     * Create a FreeColGameObjectType with a given specification but
     * no object identifier.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public FreeColGameObjectType(Specification specification) {
        this(null, specification);
    }

    /**
     * Create a FreeColGameObjectType with a given identifier and
     * specification.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public FreeColGameObjectType(String id, Specification specification) {
        setId(id);
        setSpecification(specification);
    }


    /**
     * Gets the feature container.
     *
     * @return The <code>FeatureContainer</code>.
     */
    @Override
    public final FeatureContainer getFeatureContainer() {
        if (featureContainer == null) featureContainer = new FeatureContainer();
        return featureContainer;
    }

    /**
     * Gets the index of this FreeColGameObjectType.
     *
     * The index imposes a total ordering consistent with equals on
     * each class extending FreeColGameObjectType, but this ordering
     * is nothing but the order in which the objects of the respective
     * class were defined.  It is guaranteed to remain stable only for
     * a particular revision of a particular specification.
     *
     * @return The game object index.
     */
    protected int getIndex() {
        return index;
    }

    /**
     * Sets the index of this FreeColGameObjectType.
     *
     * @param index The new index value.
     */
    protected final void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Gets a string suitable for looking up the description of
     * this object in {@link net.sf.freecol.client.gui.i18n.Messages}.
     *
     * @return A description key.
     */
    public final String getDescriptionKey() {
        return getId() + ".description";
    }

    /**
     * Gets the identifier of this object with the given prefix
     * removed if the id of the object starts with the prefix, and the
     * entire id otherwise.
     *
     * @param prefix The prefix to test.
     * @return An identifier.
     */
    public final String getSuffix(String prefix) {
        return (getId().startsWith(prefix))
            ? getId().substring(prefix.length())
            : getId();
    }

    /**
     * Gets the usual suffix of this object's identifier, that is
     * everything after the last ".".
     *
     * @return The usual identifier suffix.
     */
    public final String getSuffix() {
        String id = getId();
        return (id == null) ? null : id.substring(id.lastIndexOf('.') + 1);
    }

    /**
     * Is this an abstract type?
     *
     * @return True if this is an abstract game object type.
     */
    public final boolean isAbstractType() {
        return abstractType;
    }

    /**
     * Get the modifier index used to sort modifiers instantiated with
     * this object as the source.
     *
     * Override in subclasses that are involved in production.  Note
     * that the parameter allows the index to vary according to what
     * is being produced.
     *
     * @param id The identifier of the object being produced.
     * @return The modifier index.
     */
    public int getModifierIndex(String id) {
        return Modifier.DEFAULT_PRODUCTION_INDEX;
    }


    // Interface Named

    /**
     * Gets a string suitable for looking up the name of this
     * object in {@link net.sf.freecol.client.gui.i18n.Messages}.
     *
     * @return A message key.
     */
    public final String getNameKey() {
        return getId() + ".name";
    }


    // Serialization

    // We do not serialize index, so no INDEX_TAG.
    // We do not need to write the abstractType attribute, as once
    // the spec is read, all cases of abstractType==true are removed.
    private static final String ABSTRACT_TAG = "abstract";
    // Denotes deletion of a child element.
    protected static final String DELETE_TAG = "delete";
    // Denotes that this type extends another.
    public static final String EXTENDS_TAG = "extends";
    // Denotes preservation of attributes and children.
    public static final String PRESERVE_TAG = "preserve";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Ability ability : getSortedCopy(getAbilitySet())) {
            ability.toXML(xw);
        }

        for (Modifier modifier : getSortedModifiers()) {
            modifier.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);
        if (getId() == null) throw new XMLStreamException("Null id");

        abstractType = xr.getAttribute(ABSTRACT_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            if (featureContainer != null) featureContainer.clear();
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (Ability.getXMLElementTagName().equals(tag)) {
            if (xr.getAttribute(DELETE_TAG, false)) {
                removeAbilities(xr.readId());
                xr.closeTag(Ability.getXMLElementTagName());

            } else {
                Ability ability = new Ability(xr, spec); // Closes the element
                if (ability.getSource() == null) ability.setSource(this);
                addAbility(ability);
                spec.addAbility(ability);
            }

        } else if (Modifier.getXMLElementTagName().equals(tag)) {
            if (xr.getAttribute(DELETE_TAG, false)) {
                removeModifiers(xr.readId());
                xr.closeTag(Modifier.getXMLElementTagName());

            } else {
                Modifier modifier
                    = new Modifier(xr, spec);// Closes the element
                if (modifier.getSource() == null) modifier.setSource(this);
                if (modifier.getModifierIndex()
                    <= Modifier.DEFAULT_MODIFIER_INDEX) {
                    int index = this.getModifierIndex(modifier.getId());
                    modifier.setModifierIndex(index);
                }
                addModifier(modifier);
                spec.addModifier(modifier);
            }

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getId();
    }
}
