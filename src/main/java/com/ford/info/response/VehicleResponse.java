package com.ford.info.response;

import java.util.List;

public class VehicleResponse {
	
	private List<VehicleInfo> vehicles;

	public List<VehicleInfo> getVehicles() {
		return vehicles;
	}

	public void setVehicles(List<VehicleInfo> vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	public String toString() {
		return "Vehicles [VehicleResponse=" + vehicles + "]";
	}

	
}
