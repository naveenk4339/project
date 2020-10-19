package com.ford.info.request;

import java.util.List;

public class Vehicles {
	
	private List<VehicleRequest> vehicles;

	public List<VehicleRequest> getVehicles() {
		return vehicles;
	}

	public void setVehicles(List<VehicleRequest> vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	public String toString() {
		return "Vehicles [VehicleResponse=" + vehicles + "]";
	}

	
}
