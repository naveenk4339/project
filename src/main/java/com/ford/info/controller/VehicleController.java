package com.ford.info.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ford.info.exception.VehicleNotFoundException;
import com.ford.info.request.VehicleRequest;
import com.ford.info.request.Vehicles;
import com.ford.info.response.Response;
import com.ford.info.response.VehicleResponse;
import com.ford.info.service.VehicleService;

@RestController
@RequestMapping("/api")
public class VehicleController {

	@Autowired
	private VehicleService vehicleService;
	
	@RequestMapping("/greeting")
	public String greeting() {
		
		return "Hello Ford";
	}
	
	

	@RequestMapping("/getVehicleInformation")
	public VehicleResponse getVehicleInformation() {

		VehicleResponse vehicles = new VehicleResponse();
		
		vehicles = vehicleService.getVehicalInformation();
		
		if(vehicles == null) {
			throw new VehicleNotFoundException("Vehchile not Found");
		}

		return vehicles;

	}
	
	@RequestMapping("/getVehicleModelName/{modelName}")
	public VehicleResponse getVehicleModelName(@PathVariable String modelName) {
		
		
		VehicleResponse vehicles = new VehicleResponse();
		
		
		vehicles = vehicleService.getVehicleModelName(modelName);

		if(vehicles == null) {
			throw new VehicleNotFoundException("Model Name: "+modelName);
		}
		return vehicles;

	}
	
	@RequestMapping("/getVehiclePrice/{from}/{to}")
	public VehicleResponse getVehiclePrice(@PathVariable Double from,@PathVariable Double to) {
		
		VehicleResponse vehicles = new VehicleResponse();

		vehicles = vehicleService.getVehiclePrice(from, to);
		if(vehicles== null) {
			throw new VehicleNotFoundException("Vehicle not found From Price:  "+from+" and To Price: "+to);
		}
	
		return vehicles;

	}
	
	
	@RequestMapping("/getVehicleByFeatures/{exterior}/{interior}")
	public VehicleResponse getVehicleByFeatures(@PathVariable String exterior,@PathVariable String interior) {

		VehicleResponse vehicles = new VehicleResponse();
		
		vehicles = vehicleService.getVehicleByFeatures(exterior, interior);
		if(vehicles== null) {
			throw new VehicleNotFoundException("Vehicle not found exterior:  "+exterior+" and interior: "+interior);
		}
		return vehicles;

	}
	
	
	@PostMapping("/vehicleInfomation/submitVehicle")
	public Response submitVehicleInfo(@RequestBody VehicleRequest vechicle){
		Response response = new Response();
		
		String responseStatus  =vehicleService.saveVehicleData(vechicle);
		//System.out.println(vechicle.getVehicleId());
		if(responseStatus != null && responseStatus.equalsIgnoreCase("Success")) {
		response.setMessage("Success");
		response.setStatus("OK");
		response.setStatusCode(HttpStatus.ACCEPTED.toString());
		}else {
			response.setMessage("Not able to save the Vehicle info");
			response.setStatus("Bad resquest");
			response.setStatusCode(HttpStatus.BAD_REQUEST.toString());
		}
		return response;
	}
	
}
