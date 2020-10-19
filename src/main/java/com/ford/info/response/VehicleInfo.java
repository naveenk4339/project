package com.ford.info.response;

import com.ford.info.request.VehicleDetails;

public class VehicleInfo {
	
	private Integer vehicleId;
	private VehicleDetails vehicleDetails;
	
	
	public VehicleDetails getVehicleDetails() {
		return vehicleDetails;
	}
	public void setVehicleDetails(VehicleDetails vehicleDetails) {
		this.vehicleDetails = vehicleDetails;
	}
	public Integer getVehicleId() {
		return vehicleId;
	}
	public void setVehicleId(Integer vehicleId) {
		this.vehicleId = vehicleId;
	}
	@Override
	public String toString() {
		return "Vehicle [vehicleId=" + vehicleId + ", vehicleDetails=" + vehicleDetails + "]";
	}
	
	

}
