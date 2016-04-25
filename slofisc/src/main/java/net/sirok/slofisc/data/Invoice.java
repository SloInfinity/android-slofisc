package net.sirok.slofisc.data;

import net.sirok.slofisc.FursAPI;
import net.sirok.slofisc.NumberingStructureType;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by martin on 12.2.2016.
 */
public abstract class Invoice {
    public abstract String getZoi();
    public abstract String getTaxNumber();
    public abstract Date getDateIssued();
    public abstract String getInvoiceNumber();
    public abstract String getBusinessPremiseId();
    public abstract String getElectronicDeviceId();
    public abstract BigDecimal getInvoiceAmount();


    public BigDecimal getLowTaxRateBase(){
        return null;
    }

    public BigDecimal getLowTaxRateAmount(){
        return null;
    }

    public BigDecimal getHighTaxRateBase(){
        return null;
    }

    public BigDecimal getHighTaxRateAmount(){
        return null;
    }

    public BigDecimal getOtherTaxesAmount() {
        return null;
    }

    public BigDecimal getExemptVatTaxableAmount(){
        return null;
    }

    public BigDecimal getReverseVatTaxableAmount(){
        return null;
    }

    public BigDecimal getNonTaxableAmount(){
        return null;
    }

    public BigDecimal getSpecialTaxRulesAmount(){
        return null;
    }

    public BigDecimal getPaymentAmount(){
        return null;
    }

    public String getCustomerVatNumber(){
        return null;
    }

    public BigDecimal getReturnsAmount(){
        return null;
    }

    public String getOperatorTaxNumber(){
        return null;
    }

    public Boolean getForeignOperator(){
        return false;
    }

    public Boolean getSubsequentSubmit(){
        return false;
    }

    public String getReferenceInvoiceNumber(){
        return null;
    }

    public String getReferenceInvoiceBusinessPremiseId(){
        return null;
    }

    public String getReferenceInvoiceElectronicDeviceId(){
        return null;
    }

    public Date getReferenceInvoiceIssuedDate(){
        return null;
    }

    public String getNumberingStructure(){
        return NumberingStructureType.DEVICE;
    }

    public String getSpecialNotes(){
        return "-";
    }

}
