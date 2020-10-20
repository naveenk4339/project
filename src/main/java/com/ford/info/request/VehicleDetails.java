package com.ford.info.request;

public class VehicleDetails {
	
	private String make;
	private String  model;
	private String modelYear;
	private String bodyStyle;
	private String engine;
	private String driveType;
	private String color;
	private String mpg;
	private VehicleFeature vehicleFeature;
	private VehiclePrice vehiclePrice;
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
	public VehicleFeature getVehicleFeature() {
		return vehicleFeature;
	}
	public void setVehicleFeature(VehicleFeature vehicleFeature) {
		this.vehicleFeature = vehicleFeature;
	}
	public VehiclePrice getVehiclePrice() {
		return vehiclePrice;
	}
	public void setVehiclePrice(VehiclePrice vehiclePrice) {
		this.vehiclePrice = vehiclePrice;
	}
	@Override
	public String toString() {
		return "VehicleDetails [make=" + make + ", model=" + model + ", modelYear=" + modelYear + ", bodyStyle="
				+ bodyStyle + ", engine=" + engine + ", driveType=" + driveType + ", color=" + color + ", mpg=" + mpg
				+ ", vehicleFeature=" + vehicleFeature + ", vehiclePrice=" + vehiclePrice + "]";
	}
	
	

}
