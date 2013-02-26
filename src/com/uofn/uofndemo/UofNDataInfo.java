package com.uofn.uofndemo;

public class UofNDataInfo {
	private double my_lat;
	private double my_lon;
	private String my_updated_time;
	private String description;
	private String my_world_id;
	
	public void setMyLat(double lat)
	{
		this.my_lat = lat;
	}
	
	public double getMyLat()
	{
		return this.my_lat;
	}
	
	public void setMyLon(double lon)
	{
		this.my_lon = lon;
	}
	
	public double getMyLon()
	{
		return this.my_lon;
	}
	
	public void setMyUpdatedTime(String time)
	{
		this.my_updated_time = time;
	}
	
	public String getMyUpdatedTime()
	{
		return this.my_updated_time;
	}
	
	public void setDescription(String desc)
	{
		this.description = desc;
	}
	
	public String getDescription()
	{
		return this.description;
	}
	
	public void setMyWorldId(String id)
	{
		this.my_world_id = id;
	}
	
	public String getMyWorldId()
	{
		return this.my_world_id;
	}
}
