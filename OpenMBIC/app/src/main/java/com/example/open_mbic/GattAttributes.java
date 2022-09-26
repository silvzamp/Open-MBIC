package com.example.open_mbic;

import java.util.UUID;

public class GattAttributes {
    // UIID related to the devices, some sensor may require more UUIDs depending on their firmware

    // YOUR SENSOR 1 UUIDs - the strings here reported are just examples
    public static UUID Sensor1Service = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    public static UUID Sensor1Char = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");

    // YOUR SENSOR 2 UUIDs - the strings here reported are just examples
    // Service 1
    public static UUID Sensor2Service = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static UUID Sensor2Char = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    // Service 2
    public static UUID Sensor2Service2 = UUID.fromString("FB005C80-02E7-F387-1CAD-8ACD2D8DF0C8");
    public static UUID Sensor2CharControl = UUID.fromString("FB005C81-02E7-F387-1CAD-8ACD2D8DF0C8");
    public static UUID Sensor2CharData = UUID.fromString("FB005C82-02E7-F387-1CAD-8ACD2D8DF0C8");

    // YOUR SENSOR 3 UUIDs - the strings here reported are just examples
    public static UUID Sensor3Service = UUID.fromString("c8c0a708-e361-4b5e-a365-98fa6b0a836f");
    public static UUID Sensor3CharData = UUID.fromString("09bf2c52-d1d9-c0b7-4145-475964544307");
    public static UUID Sensor3CharControl= UUID.fromString("d5913036-2d8a-41ee-85b9-4e361aa5c8a7");

    // UUID common across all sensors
    public static UUID SensorsDescr = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


}