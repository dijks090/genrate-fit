/////////////////////////////////////////////////////////////////////////////////////////////
// Copyright 2023 Garmin International, Inc.
// Licensed under the Flexible and Interoperable Data Transfer (FIT) Protocol License; you
// may not use this file except in compliance with the Flexible and Interoperable Data
// Transfer (FIT) Protocol License.
/////////////////////////////////////////////////////////////////////////////////////////////

package nl.sander;

import com.garmin.fit.*;

import java.util.TimeZone;
import java.util.*;

public class EncodeActivity {

    public static void main(String[] args) {
        createTimeBasedActivity();
    }

    public static void createTimeBasedActivity() {
        final double twoPI = Math.PI * 2.0;
        final String filename = "ActivityEncodeRecipe.fit";

        List<Mesg> messages = new ArrayList<Mesg>();

        // The starting timestamp for the activity
        DateTime startTime = new DateTime(new Date());

        // Timer Events are a BEST PRACTICE for FIT ACTIVITY files
        EventMesg eventMesg = new EventMesg();
        eventMesg.setTimestamp(startTime);
        eventMesg.setEvent(Event.TIMER);
        eventMesg.setEventType(EventType.START);
        messages.add(eventMesg);

        // Create the Developer Id message for the developer data fields.
        DeveloperDataIdMesg developerIdMesg = new DeveloperDataIdMesg();
        // It is a BEST PRACTICE to reuse the same Guid for all FIT files created by your platform
        byte[] appId = new byte[]{
                0x1, 0x1, 0x2, 0x3,
                0x5, 0x8, 0xD, 0x15,
                0x22, 0x37, 0x59, (byte) 0x90,
                (byte) 0xE9, 0x79, 0x62, (byte) 0xDB
        };

        for (int i = 0; i < appId.length; i++) {
            developerIdMesg.setApplicationId(i, appId[i]);
        }

        developerIdMesg.setDeveloperDataIndex((short) 0);
        messages.add(developerIdMesg);

        // Create the Developer Data Field Descriptions
        FieldDescriptionMesg doughnutsFieldDescMesg = new FieldDescriptionMesg();
        doughnutsFieldDescMesg.setDeveloperDataIndex((short) 0);
        doughnutsFieldDescMesg.setFieldDefinitionNumber((short) 0);
        doughnutsFieldDescMesg.setFitBaseTypeId(FitBaseType.FLOAT32);
        doughnutsFieldDescMesg.setUnits(0, "doughnuts");
        doughnutsFieldDescMesg.setNativeMesgNum(MesgNum.SESSION);
        messages.add(doughnutsFieldDescMesg);

        FieldDescriptionMesg hrFieldDescMesg = new FieldDescriptionMesg();
        hrFieldDescMesg.setDeveloperDataIndex((short) 0);
        hrFieldDescMesg.setFieldDefinitionNumber((short) 1);
        hrFieldDescMesg.setFitBaseTypeId(FitBaseType.UINT8);
        hrFieldDescMesg.setFieldName(0, "Heart Rate");
        hrFieldDescMesg.setUnits(0, "bpm");
        hrFieldDescMesg.setNativeFieldNum((short) RecordMesg.HeartRateFieldNum);
        hrFieldDescMesg.setNativeMesgNum(MesgNum.RECORD);
        messages.add(hrFieldDescMesg);

        // Every FIT ACTIVITY file MUST contain Record messages
        DateTime timestamp = new DateTime(startTime);

        // Create a Random object
        Random random = new Random();

        // Generate and print 10 random integers between 103 and 170
        int minimalHr = 103;
        int maxHr = 145;

        int previousHr = random.nextInt(maxHr - minimalHr + 1) + minimalHr;

        // Create half an  hour (3600 seconds) of Record data
        for (int i = 0; i <= 5600; i++) {
            // Create a new Record message and set the timestamp
            RecordMesg recordMesg = new RecordMesg();
            recordMesg.setTimestamp(timestamp);

            // Fake Record Data of Various Signal Patterns
            // Generate a random integer no more than 2 away from the previous number
            int lowerBound = Math.max(minimalHr, previousHr - 2);
            int upperBound = Math.min(maxHr, previousHr + 2);
            int randomHr = random.nextInt(upperBound - lowerBound + 1) + lowerBound;
            previousHr = randomHr;
            recordMesg.setHeartRate((short)randomHr); // Sine

            // Add a Developer Field to the Record Message
            DeveloperField hrDevField = new DeveloperField(hrFieldDescMesg, developerIdMesg);
            recordMesg.addDeveloperField(hrDevField);
            hrDevField.setValue((short) (Math.sin(twoPI * (.01 * i + 10)) + 1.0) * 127.0);

            // Write the Record message to the output stream
            messages.add(recordMesg);

            // Increment the timestamp by one second
            timestamp.add(1);
        }

        // Timer Events are a BEST PRACTICE for FIT ACTIVITY files
        EventMesg eventMesgStop = new EventMesg();
        eventMesgStop.setTimestamp(timestamp);
        eventMesgStop.setEvent(Event.TIMER);
        eventMesgStop.setEventType(EventType.STOP_ALL);
        messages.add(eventMesgStop);

        // Every FIT ACTIVITY file MUST contain at least one Lap message
        LapMesg lapMesg = new LapMesg();
        lapMesg.setMessageIndex(0);
        lapMesg.setTimestamp(timestamp);
        lapMesg.setStartTime(startTime);
        lapMesg.setTotalElapsedTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
        lapMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
        messages.add(lapMesg);

        // Every FIT ACTIVITY file MUST contain at least one Session message
        SessionMesg sessionMesg = new SessionMesg();
        sessionMesg.setMessageIndex(0);
        sessionMesg.setTimestamp(timestamp);
        sessionMesg.setStartTime(startTime);
        sessionMesg.setTotalElapsedTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
        sessionMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
        sessionMesg.setSport(Sport.GENERIC);
        sessionMesg.setSubSport(SubSport.GENERIC);
        sessionMesg.setFirstLapIndex(0);
        sessionMesg.setNumLaps(1);
        messages.add(sessionMesg);

        // Add a Developer Field to the Session message
        DeveloperField doughnutsEarnedDevField = new DeveloperField(doughnutsFieldDescMesg, developerIdMesg);
        doughnutsEarnedDevField.setValue(sessionMesg.getTotalElapsedTime() / 1200.0f);
        sessionMesg.addDeveloperField(doughnutsEarnedDevField);

        // Every FIT ACTIVITY file MUST contain EXACTLY one Activity message
        ActivityMesg activityMesg = new ActivityMesg();
        activityMesg.setTimestamp(timestamp);
        activityMesg.setNumSessions(1);
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Amsterdam");
        long timezoneOffset = (timeZone.getRawOffset() + timeZone.getDSTSavings()) / 1000;
        activityMesg.setLocalTimestamp(timestamp.getTimestamp() + timezoneOffset);
        activityMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
        messages.add(activityMesg);

        createActivityFile(messages, filename, startTime);
    }


    public static void createActivityFile(List<Mesg> messages, String filename, DateTime startTime) {
        // The combination of file type, manufacturer id, product id, and serial number should be unique.
        // When available, a non-random serial number should be used.
        File fileType = File.ACTIVITY;
        short manufacturerId = Manufacturer.DEVELOPMENT;
        short productId = 0;
        float softwareVersion = 1.0f;

        Random random = new Random();
        int serialNumber = random.nextInt();

        // Every FIT file MUST contain a File ID message
        FileIdMesg fileIdMesg = new FileIdMesg();
        fileIdMesg.setType(fileType);
        fileIdMesg.setManufacturer((int) manufacturerId);
        fileIdMesg.setProduct((int) productId);
        fileIdMesg.setTimeCreated(startTime);
        fileIdMesg.setSerialNumber((long) serialNumber);

        // A Device Info message is a BEST PRACTICE for FIT ACTIVITY files
        DeviceInfoMesg deviceInfoMesg = new DeviceInfoMesg();
        deviceInfoMesg.setDeviceIndex(DeviceIndex.CREATOR);
        deviceInfoMesg.setManufacturer(Manufacturer.DEVELOPMENT);
        deviceInfoMesg.setProduct((int) productId);
        deviceInfoMesg.setProductName("FIT Cookbook"); // Max 20 Chars
        deviceInfoMesg.setSerialNumber((long) serialNumber);
        deviceInfoMesg.setSoftwareVersion(softwareVersion);
        deviceInfoMesg.setTimestamp(startTime);

        // Create the output stream
        FileEncoder encode;

        try {
            encode = new FileEncoder(new java.io.File(filename), Fit.ProtocolVersion.V2_0);
        } catch (FitRuntimeException e) {
            System.err.println("Error opening file " + filename);
            e.printStackTrace();
            return;
        }

        encode.write(fileIdMesg);
        encode.write(deviceInfoMesg);

        for (Mesg message : messages) {
            encode.write(message);
        }

        // Close the output stream
        try {
            encode.close();
        } catch (FitRuntimeException e) {
            System.err.println("Error closing encode.");
            e.printStackTrace();
            return;
        }
        System.out.println("Encoded FIT Activity file " + filename);
    }


}
