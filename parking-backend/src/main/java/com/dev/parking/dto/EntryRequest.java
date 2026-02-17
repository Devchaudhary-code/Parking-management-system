package com.dev.parking.dto;

import com.dev.parking.entity.VehicleType;

public class EntryRequest {
    private String plate;
    private VehicleType type;

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public VehicleType getType() { return type; }
    public void setType(VehicleType type) { this.type = type; }
}
