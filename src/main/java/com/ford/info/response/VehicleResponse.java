package com.ford.info.response;

import java.util.List;

public class VehicleResponse {
	
	private String message;
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	
}
