package com.ford.info.controller;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Role;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

	@GetMapping("/greeting")
	public String greeting() {

		return "Hello Ford";
	}

	@Value("vehical.modelname.required")
	private String modelNameRequiredMgs;

	@Value("vehical.price.range")
	private String priceRangeRequriedMgs;

	@Value("vehical.features.exterior.interior")
	private String featuresRequiedMgs;

	@GetMapping("/getVehicleInformation")
//	@RolesAllowed( "Admin" )
	public VehicleResponse getVehicleInformation() {

		VehicleResponse vehicles = new VehicleResponse();
		vehicles = vehicleService.getVehicalInformation();

		if (vehicles == null) {
			throw new VehicleNotFoundException("Vehchile not Found");
		}

		if (vehicles.getVehicles().size() > 0)
			vehicles.setMessage("Success");

		return vehicles;

	}

	@SuppressWarnings("unused")
	@GetMapping("/getVehicleModelName/{modelName}")
	public VehicleResponse getVehicleModelName(@Validated @PathVariable String modelName) {

		VehicleResponse vehicles = new VehicleResponse();

		if (!modelName.isEmpty()) {
			vehicles = vehicleService.getVehicleModelName(modelName);

		} else {
			vehicles.setMessage(modelNameRequiredMgs);
		}

		if (vehicles == null) {
			throw new VehicleNotFoundException("Model Name: " + modelName);
		}

		if (vehicles.getVehicles().size() > 0)
			vehicles.setMessage("Success");
		return vehicles;

	}

	@GetMapping("/getVehiclePrice/{from}/{to}")
	public VehicleResponse getVehiclePrice(@Validated @PathVariable Double from, @Validated @PathVariable Double to) {

		VehicleResponse vehicles = new VehicleResponse();
		if (!(from.isNaN() && to.isNaN())) {
			vehicles = vehicleService.getVehiclePrice(from, to);
		} else {

			vehicles.setMessage(priceRangeRequriedMgs);
		}

		if (vehicles == null) {
			throw new VehicleNotFoundException("Vehicle not found From Price:  " + from + " and To Price: " + to);
		}

		if (vehicles.getVehicles().size() > 0)
			vehicles.setMessage("Success");
		return vehicles;

	}

	@GetMapping("/getVehicleByFeatures/{exterior}/{interior}")
	public VehicleResponse getVehicleByFeatures(@Validated @PathVariable String exterior,
			@Validated @PathVariable String interior) {

		VehicleResponse vehicles = new VehicleResponse();

		vehicles = vehicleService.getVehicleByFeatures(exterior, interior);

		if (vehicles == null) {
			throw new VehicleNotFoundException(
					"Vehicle not found exterior:  " + exterior + " and interior: " + interior);
		}
		if (vehicles.getVehicles().size() > 0)
			vehicles.setMessage("Success");
		return vehicles;

	}

	@PostMapping("/vehicleInfomation/submitVehicle")
	public Response submitVehicleInfo(@RequestBody VehicleRequest vechicle) {
		Response response = new Response();

		String responseStatus = vehicleService.saveVehicleData(vechicle);
		// System.out.println(vechicle.getVehicleId());
		if (responseStatus != null && responseStatus.equalsIgnoreCase("Success")) {
			response.setMessage("Success");
			response.setStatus("OK");
			response.setStatusCode(HttpStatus.ACCEPTED.toString());
		} else {
			response.setMessage("Not able to save the Vehicle info");
			response.setStatus("Bad resquest");
			response.setStatusCode(HttpStatus.BAD_REQUEST.toString());
		}
		return response;
	}

}
