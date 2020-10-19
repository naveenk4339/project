package com.ford.info.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ford.info.model.Vehicle;

@Repository
public interface VehicleRepository extends CrudRepository<Vehicle, Integer> {
	
	List<Vehicle> findByModel(String model);
	
	List<Vehicle> findByMsrpGreaterThanEqualAndMsrpLessThanEqual(Double first, Double second);
	


}
