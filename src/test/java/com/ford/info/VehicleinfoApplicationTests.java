package com.ford.info;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.ford.info.controller.VehicleController;
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
	
	
	@Test
	public void testSizeRequestVechilesList() {
		VehicleResponse vehicleResponse = new VehicleResponse();
		List<VehicleInfo> vehicalInfoList = new ArrayList<VehicleInfo>();
		VehicleInfo vehicleInfos = new VehicleInfo();
		VehicleDetails details = new VehicleDetails();
		details.setMake("Ford");
		details.setMake("Ford");
		details.setModel("Escape");
		details.setModelYear("2019");
		details.setBodyStyle("Sedan");
		details.setEngine("4D Sport Utility");
		details.setDriveType("AWD");
		details.setColor("Agate Black");
		details.setMPG("28");
		
		vehicleInfos.setVehicleDetails(details);
		vehicalInfoList.add(vehicleInfos);
		vehicleResponse.setVehicles(vehicalInfoList);
		
		
		Mockito.when(vehicleController.getVehicleInformation()).thenReturn(vehicleResponse);
		assertEquals(vehicalInfoList.size(),
				vehicleService.getVehicalInformation().getVehicles().size());
	
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
		vehicleDetails.setMPG("28");
		
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
	

		vehiclePrice.setFinalPrice(20000.00);
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
	
	
	 
	  
//	  @Test(expected = VehicleNotFoundException.class)
//	  @After
//	  public void vehicleNotFoundExceptionTest() throws VehicleNotFoundException {
//	     
//	      vehicleController.getVehicleInformation().setVehicles(null);
//	      
//	  }

	
	
	

}
