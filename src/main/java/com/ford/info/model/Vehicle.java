package com.ford.info.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Vehicle {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Integer id;
	@Column(name = "MAKE")
	private String make;

	@Column(name = "MODEL")
	private String model;

	@Column(name = "MODELYEAR")
	private String modelYear;

	@Column(name = "BODYSTYLE")
	private String bodyStyle;
	@Column(name = "ENGINE")
	private String engine;
	@Column(name = "DRIVETYPE")
	private String driveType;
	@Column(name = "COLOR")
	private String color;
	@Column(name = "MPG")
	private String mpg;
	@Column(name = "EXTERIOR", length = 1000)
	private String exterior;
	@Column(name = "INTERIOR", length = 1000)
	private String interior;
	@Column(name = "MSRP")
	private Double msrp;
	@Column(name = "SAVINGS")
	private Double savings;
	@Column(name = "FINALPRICE")
	private Double finalPrice;

	public Vehicle() {
		// TODO Auto-generated constructor stub
	}

	
	

	public Vehicle(Integer id, String make, String model, String modelYear, String bodyStyle, String engine,
			String driveType, String color, String mpg, String exterior, String interior, Double msrp, Double savings,
			Double finalPrice) {
		super();
		this.id = id;
		this.make = make;
		this.model = model;
		this.modelYear = modelYear;
		this.bodyStyle = bodyStyle;
		this.engine = engine;
		this.driveType = driveType;
		this.color = color;
		this.mpg = mpg;
		this.exterior = exterior;
		this.interior = interior;
		this.msrp = msrp;
		this.savings = savings;
		this.finalPrice = finalPrice;
	}


	public Integer getId() {
		return id;
	}




	public void setId(Integer id) {
		this.id = id;
	}




	public String getMake() {
		return make;
	}




	public void setMake(String make) {
		this.make = make;
	}




	public String getModel() {
		return model;
	}




	public void setModel(String model) {
		this.model = model;
	}




	public String getModelYear() {
		return modelYear;
	}




	public void setModelYear(String modelYear) {
		this.modelYear = modelYear;
	}




	public String getBodyStyle() {
		return bodyStyle;
	}




	public void setBodyStyle(String bodyStyle) {
		this.bodyStyle = bodyStyle;
	}




	public String getEngine() {
		return engine;
	}




	public void setEngine(String engine) {
		this.engine = engine;
	}




	public String getDriveType() {
		return driveType;
	}




	public void setDriveType(String driveType) {
		this.driveType = driveType;
	}




	public String getColor() {
		return color;
	}




	public void setColor(String color) {
		this.color = color;
	}




	public String getMpg() {
		return mpg;
	}




	public void setMpg(String mpg) {
		this.mpg = mpg;
	}




	public String getExterior() {
		return exterior;
	}




	public void setExterior(String exterior) {
		this.exterior = exterior;
	}




	public String getInterior() {
		return interior;
	}




	public void setInterior(String interior) {
		this.interior = interior;
	}




	public Double getMsrp() {
		return msrp;
	}




	public void setMsrp(Double msrp) {
		this.msrp = msrp;
	}




	public Double getSavings() {
		return savings;
	}




	public void setSavings(Double savings) {
		this.savings = savings;
	}




	public Double getFinalPrice() {
		return finalPrice;
	}




	public void setFinalPrice(Double finalPrice) {
		this.finalPrice = finalPrice;
	}




	@Override
	public String toString() {
		return "Vehicle [id=" + id + ", make=" + make + ", model=" + model + ", modelYear=" + modelYear + ", bodyStyle="
				+ bodyStyle + ", engine=" + engine + ", driveType=" + driveType + ", color=" + color + ", mpg=" + mpg
				+ ", exterior=" + exterior + ", interior=" + interior + ", msrp=" + msrp + ", savings=" + savings
				+ ", finalPrice=" + finalPrice + "]";
	}
	
	

}
