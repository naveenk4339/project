package com.ford.info.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ford.info.dao.VehicleRepository;
import com.ford.info.model.Vehicle;
import com.ford.info.request.VehicleDetails;
import com.ford.info.request.VehicleFeature;
import com.ford.info.request.VehiclePrice;
import com.ford.info.request.VehicleRequest;
import com.ford.info.response.VehicleInfo;
import com.ford.info.response.VehicleResponse;

@Service
public class VehicleServiceImpl implements VehicleService {

	@Autowired
	private VehicleRepository vehicleRepository;

	@Override
	@javax.transaction.Transactional
	public VehicleResponse getVehicalInformation() {
		

		VehicleResponse vehicleResponse = new VehicleResponse();

		List<VehicleInfo> vehicleResponseList = new ArrayList<VehicleInfo>();

		List<Vehicle> vehiclesList = new ArrayList<Vehicle>();

		vehicleRepository.findAll().forEach(vehiclesList::add);

		for (Vehicle vehicle : vehiclesList) {

			VehicleInfo vehicleInfoResponse = new VehicleInfo();
			vehicleInfoResponse.setVehicleId(vehicle.getId());
			VehicleDetails vehicleDetails = new VehicleDetails();
			vehicleDetails.setMake(vehicle.getMake());
			vehicleDetails.setModel(vehicle.getModel());
			vehicleDetails.setModelYear(vehicle.getModelYear());
			vehicleDetails.setBodyStyle(vehicle.getBodyStyle());
			vehicleDetails.setEngine(vehicle.getEngine());
			vehicleDetails.setDriveType(vehicle.getDriveType());
			vehicleDetails.setColor(vehicle.getColor());
			vehicleDetails.setMpg(vehicle.getMpg());

			VehicleFeature vehicleFeature = new VehicleFeature();
			String[] exterior = vehicle.getExterior().split(",");
			vehicleFeature.setExterior(Arrays.asList(exterior));
			String[] interior = vehicle.getInterior().split(",");
			vehicleFeature.setInterior(Arrays.asList(interior));
			vehicleDetails.setVehicleFeature(vehicleFeature);

			VehiclePrice vehiclePrice = new VehiclePrice();
			vehiclePrice.setFinalPrice(vehicle.getFinalPrice());
			vehiclePrice.setSavings(vehicle.getSavings());
			vehiclePrice.setMSRP(vehicle.getMsrp());
			vehicleDetails.setVehiclePrice(vehiclePrice);

			vehicleInfoResponse.setVehicleDetails(vehicleDetails);

			vehicleResponseList.add(vehicleInfoResponse);
		}

		vehicleResponse.setVehicles(vehicleResponseList);

		return vehicleResponse;
	}

	@Override
	@javax.transaction.Transactional
	public VehicleResponse getVehicleModelName(String modelName) {
		// TODO Auto-generated method stub
		List<Vehicle> vehiclesList = new ArrayList<Vehicle>();
		VehicleResponse vehicleResponse = new VehicleResponse();

		List<VehicleInfo> vehicleResponseList = new ArrayList<VehicleInfo>();

		vehicleRepository.findByModel(modelName).forEach(vehiclesList::add);

		for (Vehicle vehicle : vehiclesList) {

			VehicleInfo vehicleInfoResponse = new VehicleInfo();
			vehicleInfoResponse.setVehicleId(vehicle.getId());
		
			VehicleDetails vehicleDetails = new VehicleDetails();
			vehicleDetails.setMake(vehicle.getMake());
			vehicleDetails.setModel(vehicle.getModel());
			vehicleDetails.setModelYear(vehicle.getModelYear());
			vehicleDetails.setBodyStyle(vehicle.getBodyStyle());
			vehicleDetails.setEngine(vehicle.getEngine());
			vehicleDetails.setDriveType(vehicle.getDriveType());
			vehicleDetails.setColor(vehicle.getColor());
			vehicleDetails.setMpg(vehicle.getMpg());

			VehicleFeature vehicleFeature = new VehicleFeature();
			String[] exterior = vehicle.getExterior().split(",");
			vehicleFeature.setExterior(Arrays.asList(exterior));
			String[] interior = vehicle.getInterior().split(",");
			vehicleFeature.setInterior(Arrays.asList(interior));
			vehicleDetails.setVehicleFeature(vehicleFeature);

			VehiclePrice vehiclePrice = new VehiclePrice();
			vehiclePrice.setFinalPrice(vehicle.getFinalPrice());
			vehiclePrice.setSavings(vehicle.getSavings());
			vehiclePrice.setMSRP(vehicle.getMsrp());
			vehicleDetails.setVehiclePrice(vehiclePrice);

			vehicleInfoResponse.setVehicleDetails(vehicleDetails);

			vehicleResponseList.add(vehicleInfoResponse);
		}

		vehicleResponse.setVehicles(vehicleResponseList);

		return vehicleResponse;
	}

	@Override
	@javax.transaction.Transactional
	public VehicleResponse getVehiclePrice(Double from, Double to) {
		List<Vehicle> vehiclesList = new ArrayList<Vehicle>();
		VehicleResponse vehicleResponse = new VehicleResponse();

		List<VehicleInfo> vehicleResponseList = new ArrayList<VehicleInfo>();

		vehicleRepository.findByMsrpGreaterThanEqualAndMsrpLessThanEqual(from, to).forEach(vehiclesList::add);

		for (Vehicle vehicle : vehiclesList) {

			VehicleInfo vehicleInfoResponse = new VehicleInfo();
			vehicleInfoResponse.setVehicleId(vehicle.getId());
		
			VehicleDetails vehicleDetails = new VehicleDetails();
			vehicleDetails.setMake(vehicle.getMake());
			vehicleDetails.setModel(vehicle.getModel());
			vehicleDetails.setModelYear(vehicle.getModelYear());
			vehicleDetails.setBodyStyle(vehicle.getBodyStyle());
			vehicleDetails.setEngine(vehicle.getEngine());
			vehicleDetails.setDriveType(vehicle.getDriveType());
			vehicleDetails.setColor(vehicle.getColor());
			vehicleDetails.setMpg(vehicle.getMpg());

			VehicleFeature vehicleFeature = new VehicleFeature();
			String[] exterior = vehicle.getExterior().split(",");
			vehicleFeature.setExterior(Arrays.asList(exterior));
			String[] interior = vehicle.getInterior().split(",");
			vehicleFeature.setInterior(Arrays.asList(interior));
			vehicleDetails.setVehicleFeature(vehicleFeature);

			VehiclePrice vehiclePrice = new VehiclePrice();
			vehiclePrice.setFinalPrice(vehicle.getFinalPrice());
			vehiclePrice.setSavings(vehicle.getSavings());
			vehiclePrice.setMSRP(vehicle.getMsrp());
			vehicleDetails.setVehiclePrice(vehiclePrice);

			vehicleInfoResponse.setVehicleDetails(vehicleDetails);

			vehicleResponseList.add(vehicleInfoResponse);
		}

		vehicleResponse.setVehicles(vehicleResponseList);

		return vehicleResponse;
	}

	@Override
	@javax.transaction.Transactional
	public VehicleResponse getVehicleByFeatures(String exterior, String interior) {

		VehicleResponse vehicleResponse = new VehicleResponse();
		List<VehicleInfo> vehicles = new ArrayList<VehicleInfo>();
		VehicleResponse vehicle = getVehicalInformation();
				
		vehicles = vehicle.getVehicles().stream()
				.filter(data -> data.getVehicleDetails().getVehicleFeature().getExterior().contains(exterior)
						&& data.getVehicleDetails().getVehicleFeature().getInterior().contains(interior))
				.collect(Collectors.toList());
		
		vehicleResponse.setVehicles(vehicles);

		return vehicleResponse;
	}

	@Override
	@javax.transaction.Transactional
	public String saveVehicleData(VehicleRequest vechicleRequest) {

		Vehicle vehicle = new Vehicle();
		String response = "";
		try {

			vehicle.setMake(vechicleRequest.getVehicleDetails().getMake());
			vehicle.setModel(vechicleRequest.getVehicleDetails().getModel());
			vehicle.setModelYear(vechicleRequest.getVehicleDetails().getModelYear());
			vehicle.setBodyStyle(vechicleRequest.getVehicleDetails().getBodyStyle());
			vehicle.setEngine(vechicleRequest.getVehicleDetails().getEngine());
			vehicle.setDriveType(vechicleRequest.getVehicleDetails().getDriveType());
			vehicle.setColor(vechicleRequest.getVehicleDetails().getColor());
			vehicle.setMpg(vechicleRequest.getVehicleDetails().getMpg());

			List<String> exteriorList = vechicleRequest.getVehicleDetails().getVehicleFeature().getExterior();

			String exterior = String.join(",", exteriorList);
			vehicle.setExterior(exterior);

			List<String> interiorList = vechicleRequest.getVehicleDetails().getVehicleFeature().getInterior();
		
			String interior = String.join(",", interiorList);
		
			vehicle.setInterior(interior);

			vehicle.setFinalPrice(vechicleRequest.getVehicleDetails().getVehiclePrice().getFinalPrice());
			vehicle.setSavings(vechicleRequest.getVehicleDetails().getVehiclePrice().getSavings());
			vehicle.setMsrp(vechicleRequest.getVehicleDetails().getVehiclePrice().getMSRP());

			vehicleRepository.save(vehicle);
			response = "Success";

		} catch (Exception e) {
			e.printStackTrace();
			response = "Fail";
		}

		return response;

	}

}
