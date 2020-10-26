package com.ford.info;


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.ford.info.controller.VehicleController;
import com.ford.info.exception.VehicleNotFoundException;
import com.ford.info.request.VehicleDetails;
import com.ford.info.request.VehicleFeature;
import com.ford.info.request.VehiclePrice;
import com.ford.info.request.VehicleRequest;
import com.ford.info.response.Response;
import com.ford.info.response.VehicleInfo;
import com.ford.info.response.VehicleResponse;
import com.ford.info.service.VehicleService;

@RunWith(org.mockito.runners.MockitoJUnitRunner.class)
public class VehicleinfoApplicationTests {
		
	@Mock
	private VehicleService vehicleService;
	
	@InjectMocks
	private VehicleController vehicleController;
	
	
	
	@org.junit.Test(expected = VehicleNotFoundException.class)
	public void testForVehicleNotFoundException() {
	
		Mockito.when(vehicleController.getVehicleInformation()).thenThrow(VehicleNotFoundException.class);
		vehicleService.getVehicalInformation();
		 		    
	}
	
	@SuppressWarnings("unchecked")
	@org.junit.Test
	public void gettinVehcilesList() {
	
		VehicleResponse vehicleResponse = new VehicleResponse();
		
		VehicleInfo vehicleInfo = new VehicleInfo();
		
		List<VehicleInfo> infoList = new ArrayList<VehicleInfo>();
		
		VehicleDetails vehicleDetails = new VehicleDetails();
		VehicleFeature vehicleFeature = new VehicleFeature();
		VehiclePrice vehiclePrice = new VehiclePrice();
		
		vehicleDetails.setMake("Ford");
		vehicleDetails.setModel("Escape");
		vehicleDetails.setModelYear("2019");
		vehicleDetails.setBodyStyle("Sedan");
		vehicleDetails.setEngine("4D Sport Utility");
		vehicleDetails.setDriveType("AWD");
		vehicleDetails.setColor("Agate Black");
		vehicleDetails.setMpg("28");
		
		List<String> exteriorList = new ArrayList<>();
		exteriorList.add("Beltline Molding - Black");
		exteriorList.add("Door Handles - Body Color");
		exteriorList.add("Grille - Chrome");
		exteriorList.add("Taillamps-Led");
		vehicleFeature.setExterior(exteriorList);


		List<String> interiorList = new ArrayList<>();
		interiorList.add("Beltline Molding - Black");
		interiorList.add("Door Handles - Body Color");
		interiorList.add("Grille - Chrome");
		interiorList.add("Taillamps-Led");
		vehicleFeature.setInterior(interiorList);
	

		vehiclePrice.setFinalPrice(200000.00);
		vehiclePrice.setSavings(2000.00);
		vehiclePrice.setMSRP(35000.00);
		
		vehicleInfo.setVehicleDetails(vehicleDetails);
		vehicleInfo.getVehicleDetails().setVehicleFeature(vehicleFeature);
		vehicleInfo.getVehicleDetails().setVehiclePrice(vehiclePrice);
		infoList.add(vehicleInfo);
		
		vehicleResponse.setVehicles(infoList);
		
		
		Mockito.when(vehicleService.getVehicalInformation()).thenReturn(vehicleResponse);
		
		assertEquals(vehicleController.getVehicleInformation().getVehicles(), vehicleResponse.getVehicles());
			
		assertAll(() -> assertEquals(1, vehicleResponse.getVehicles().stream().count())
				);
		
	}

	
	
	@Test
	public void testSubmitVehicleData() {
		VehicleRequest vehicleRequest = new VehicleRequest();
		VehicleDetails vehicleDetails = new VehicleDetails();
		VehicleFeature vehicleFeature = new VehicleFeature();
		VehiclePrice vehiclePrice = new VehiclePrice();
		
		vehicleDetails.setMake("Ford");
		vehicleDetails.setModel("Escape");
		vehicleDetails.setModelYear("2019");
		vehicleDetails.setBodyStyle("Sedan");
		vehicleDetails.setEngine("4D Sport Utility");
		vehicleDetails.setDriveType("AWD");
		vehicleDetails.setColor("Agate Black");
		vehicleDetails.setMpg("28");
		
		List<String> exteriorList = new ArrayList<>();
		exteriorList.add("Beltline Molding - Black");
		exteriorList.add("Door Handles - Body Color");
		exteriorList.add("Grille - Chrome");
		exteriorList.add("Taillamps-Led");
		vehicleFeature.setExterior(exteriorList);


		List<String> interiorList = new ArrayList<>();
		interiorList.add("Beltline Molding - Black");
		interiorList.add("Door Handles - Body Color");
		interiorList.add("Grille - Chrome");
		interiorList.add("Taillamps-Led");
		vehicleFeature.setInterior(interiorList);
	

		vehiclePrice.setFinalPrice(200000.00);
		vehiclePrice.setSavings(2000.00);
		vehiclePrice.setMSRP(35000.00);
		
		vehicleRequest.setVehicleDetails(vehicleDetails);
		vehicleDetails.setVehicleFeature(vehicleFeature);
		vehicleDetails.setVehiclePrice(vehiclePrice);
		Response response = new Response();
		response.setMessage("Success");
		response.setStatus("OK");
		response.setStatusCode(HttpStatus.ACCEPTED.toString());
		
		Mockito.when(vehicleController.submitVehicleInfo(vehicleRequest).getStatusCode()).thenReturn(HttpStatus.ACCEPTED.toString());
		assertEquals(response.getStatusCode(), vehicleService.saveVehicleData(vehicleRequest));
	
		
	}
	
	@SuppressWarnings("unchecked")
	@org.junit.Test(expected = NullPointerException.class)
	public void submittingNullValueThrowsNullPoint() {
		VehicleRequest vehicleRequest = null;
		Mockito.when(vehicleController.submitVehicleInfo(vehicleRequest)).thenThrow(NullPointerException.class);
		vehicleService.saveVehicleData(vehicleRequest);
	}
	 
	
	

}
