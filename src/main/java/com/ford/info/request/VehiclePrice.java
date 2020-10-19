package com.ford.info.request;

public class VehiclePrice {
	
	private Double MSRP;
	private Double savings;
	private Double finalPrice;
	
	public Double getMSRP() {
		return MSRP;
	}
	public void setMSRP(Double mSRP) {
		MSRP = mSRP;
	}
	public Double getSavings() {
		return savings;
	}
	public void setSavings(Double savings) {
		this.savings = savings;
	}
	public Double getFinalPrice() {
		return finalPrice;
	}
	public void setFinalPrice(Double finalPrice) {
		this.finalPrice = finalPrice;
	}
	@Override
	public String toString() {
		return "VehiclePrice [MSRP=" + MSRP + ", savings=" + savings + ", finalPrice=" + finalPrice + "]";
	}

}
