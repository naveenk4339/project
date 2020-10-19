package com.ford.info.service;

import com.ford.info.request.VehicleRequest;
import com.ford.info.response.VehicleResponse;

public interface VehicleService {
	
	public VehicleResponse getVehicalInformation();

	public VehicleResponse getVehicleModelName(String modelName);

	public VehicleResponse getVehiclePrice(Double from, Double to);

	public VehicleResponse getVehicleByFeatures(String exterior, String interior);

	public String saveVehicleData(VehicleRequest vechicle);

}
