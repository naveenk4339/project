package com.ford.info;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.ford.info.dao.VehicleRepository;
import com.ford.info.model.Vehicle;

@DataJpaTest
@AutoConfigureTestDatabase(replace= Replace.NONE)
public class VehicleReposityTest {
	
	@Autowired
	private VehicleRepository repo;
	
	@Autowired
	private TestEntityManager entityManager;
	
	@org.junit.jupiter.api.Test
	public void testCreateVechiles() {
		
		Vehicle saveVechile = repo.save(new Vehicle(1,
				"Ford",
				"Escape",
				"2019",
				"Sedan",
				"4D Sport Utility",
				"AWD",
				"Agate Black",
				"28",
				"Beltline Molding - Black, Door Handles - Body Color,Grille - Chrome, Taillamps-Led ",
				"Beltline Molding - Black, Door Handles - Body Color, Grille - Chrome, Taillamps-Led",
				40000.00,2000.00,35000.00 ));
		
		assertThat(saveVechile.getId()).isGreaterThan(0);
		
	}
	
	@org.junit.jupiter.api.Test
	public void getVechilesList() {
		
		Iterable<Vehicle> vehicleList= repo.findAll();
		
		assertThat(vehicleList).isEmpty();
	}
	
	
	@org.junit.jupiter.api.Test
	public void getVechilesListById() {
		
		Vehicle vehicle = new Vehicle(1,
				"Ford",
				"Escape",
				"2019",
				"Sedan",
				"4D Sport Utility",
				"AWD",
				"Agate Black",
				"28",
				"Beltline Molding - Black, Door Handles - Body Color,Grille - Chrome, Taillamps-Led ",
				"Beltline Molding - Black, Door Handles - Body Color, Grille - Chrome, Taillamps-Led",
				40000.00,2000.00,35000.00 );
		entityManager.merge(vehicle);
		
		Vehicle vehicle1 = new Vehicle(2,
				"Ford",
				"Car",
				"2019",
				"Sedan",
				"4D Sport Utility",
				"AWD",
				"Agate Black",
				"28",
				"Beltline Molding - Black, Door Handles - Body Color,Grille - Chrome, Taillamps-Led ",
				"Beltline Molding - Black, Door Handles - Body Color, Grille - Chrome, Taillamps-Led",
				40000.00,2000.00,35000.00 );
		
		entityManager.merge(vehicle1);
		
		Iterable<Vehicle> foundVechiles = repo.findByModel("Car");
		
		assertThat(foundVechiles).hasSize(1);
	}
	
	  @Test
	  public void deleteAllVehicles() {
		  Vehicle vehicle = new Vehicle(1,
					"Ford",
					"Escape",
					"2019",
					"Sedan",
					"4D Sport Utility",
					"AWD",
					"Agate Black",
					"28",
					"Beltline Molding - Black, Door Handles - Body Color,Grille - Chrome, Taillamps-Led ",
					"Beltline Molding - Black, Door Handles - Body Color, Grille - Chrome, Taillamps-Led",
					40000.00,2000.00,35000.00 );
			entityManager.merge(vehicle);
			
			Vehicle vehicle1 = new Vehicle(2,
					"Ford",
					"Car",
					"2019",
					"Sedan",
					"4D Sport Utility",
					"AWD",
					"Agate Black",
					"28",
					"Beltline Molding - Black, Door Handles - Body Color,Grille - Chrome, Taillamps-Led ",
					"Beltline Molding - Black, Door Handles - Body Color, Grille - Chrome, Taillamps-Led",
					40000.00,2000.00,35000.00 );
	    
			entityManager.merge(vehicle1);

	    repo.deleteAll();

	    assertThat(repo.findAll()).isEmpty();
	  }

}
