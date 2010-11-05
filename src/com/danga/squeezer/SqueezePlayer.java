package com.danga.squeezer;

public class SqueezePlayer {
	private String playerId;
	private String ip;
	private String name;
	private boolean canpoweroff;
	private String model;
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String id) {
		this.playerId = id;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isCanpoweroff() {
		return canpoweroff;
	}
	public void setCanpoweroff(boolean canpoweroff) {
		this.canpoweroff = canpoweroff;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	
	public String toString() {
		return "id=" + playerId + ", name=" + name + ", model=" + model + ", canpoweroff=" + canpoweroff + ", ip=" + ip;
	}

}
