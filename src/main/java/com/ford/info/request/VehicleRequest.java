package com.ford.info.request;

public class VehicleRequest {
	
//	private Integer vehicleId;
	private VehicleDetails vehicleDetails;
	
	
	public VehicleDetails getVehicleDetails() {
		return vehicleDetails;
	}
	public void setVehicleDetails(VehicleDetails vehicleDetails) {
		this.vehicleDetails = vehicleDetails;
	}
//	public Integer getVehicleId() {
//		return vehicleId;
//	}
//	public void setVehicleId(Integer vehicleId) {
//		this.vehicleId = vehicleId;
//	}
	@Override
	public String toString() {
		return " vehicleDetails=" + vehicleDetails + "]";
	}
	
	

}
