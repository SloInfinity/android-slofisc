package net.sirok.slofisc.data;

import java.util.Date;

/**
 * Created by martin on 22.2.2016.
 */
public abstract class BusinessPremise {
    /**
     * Tax number of the business. E.g. "10039856".
     *
     * @return business' tax number
     */
    public abstract String getTaxNumber();

    /**
     * Business Premise Identifier. E.g. "PE12".
     *
     * @return business premise id
     */
    public abstract String getPremiseId();

    /**
     * Cadastral number of the real estate. E.g. "365".
     *
     * @return cadastral number
     */
    public abstract int getRealEstateCadastralNumber();

    /**
     * Cadastral building number. E.g. "12".
     *
     * @return cadastral building number
     */
    public abstract int getRealEstateBuildingNumber();

    /**
     * Cadastral building section number. E.g. "3".
     *
     * @return cadastral building section number
     */
    public abstract int getRealEstateBuildingSectionNumber();

    /**
     * Street name of the premise. E.g. "Slovenska cesta".
     *
     * @return street name
     */
    public abstract String getStreet();

    /**
     * House number of the premise. E.g "24".
     *
     * @return house number
     */
    public abstract String getHouseNumber();

    /**
     * Additional house number. Empty string if does not exist. E.g. "B".
     *
     * @return additional house number
     */
    public abstract String getHouseNumberAdditional();

    /**
     * Name of the town. E.g "Ljubljana".
     *
     * @return towns name
     */
    public abstract String getCommunity();

    /**
     * Name of the post office. E.g. "Ljubljana".
     *
     * @return tame of the post office
     */
    public abstract String getCity();

    /**
     * Post office number. E.g. "1000".
     *
     * @return post office number
     */
    public abstract String getPostalCode();

    /**
     * Type of the movable business unit. Valid values "A", "B" or "C".
     * Types are defined in MovablePremiseType class.
     *
     * @return type of the movable business unit
     */
    public abstract String getMovableType();

    /**
     * Datetime object representing the date when the premise started
     * issuing invoices.
     *
     * @return start date od issuing invoices
     */
    public abstract Date getValidityDate();

    /**
     * Tax number of the software supplier. E.g. "10039856"
     * Default value is null.
     *
     * @return tax number of the software supplier
     */
    public String getSoftwareSupplierTaxNumber(){
        return null;
    }

    /**
     * If software supplier is foreign company - does not have
     * Slovenian Tax number, then please provide provider name.
     * Default value is null.
     *
     * @return foreign software company name
     */
    public String getForeignSoftwareSupplierName(){
        return null;
    }

    /**
     * If you need to send any special notes to FURS. Default is "No notes"
     *
     * @return special notes
     */
    public String getSpecialNotes(){
        return "No notes";
    }
}
