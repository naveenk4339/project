package com.ford.info.request;

import java.util.List;

public class VehicleFeature {
	
	private List<String> exterior;
	private List<String> interior;
	public List<String> getExterior() {
		return exterior;
	}
	public void setExterior(List<String> exterior) {
		this.exterior = exterior;
	}
	public List<String> getInterior() {
		return interior;
	}
	public void setInterior(List<String> interior) {
		this.interior = interior;
	}
	@Override
	public String toString() {
		return "VehicleFeature [exterior=" + exterior + ", interior=" + interior + "]";
	}
	

}
