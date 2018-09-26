package co.codewizards.raspi1;

import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbPort;
import javax.usb.UsbServices;

public class UsbDumpDeviceDemo implements Runnable {

	public UsbDumpDeviceDemo(String[] args) {
	}

	@Override
	public void run() {
		try {
			// Get the USB services and dump information about them
			final UsbServices services = UsbHostManager.getUsbServices();
			System.out.println("USB Service Implementation: "
					+ services.getImpDescription());
			System.out.println("Implementation version: "
					+ services.getImpVersion());
			System.out.println("Service API version: " + services.getApiVersion());
			System.out.println();

			// Dump the root USB hub
			dumpDevice(services.getRootUsbHub());
		} catch (Exception x) {
			x.printStackTrace();
		}
	}



	private void dumpDevice(final UsbDevice device)
	{
		// Dump information about the device itself
		System.out.println(device);
		final UsbPort port = device.getParentUsbPort();
		if (port != null)
		{
			System.out.println("Connected to port: " + port.getPortNumber());
			System.out.println("Parent: " + port.getUsbHub());
		}

		// Dump device descriptor
		System.out.println(device.getUsbDeviceDescriptor());

		// Process all configurations
		for (UsbConfiguration configuration: (List<UsbConfiguration>) device
				.getUsbConfigurations())
		{
			// Dump configuration descriptor
			System.out.println(configuration.getUsbConfigurationDescriptor());

			// Process all interfaces
			for (UsbInterface iface: (List<UsbInterface>) configuration
					.getUsbInterfaces())
			{
				// Dump the interface descriptor
				System.out.println(iface.getUsbInterfaceDescriptor());

				// Process all endpoints
				for (UsbEndpoint endpoint: (List<UsbEndpoint>) iface
						.getUsbEndpoints())
				{
					// Dump the endpoint descriptor
					System.out.println(endpoint.getUsbEndpointDescriptor());
				}
			}
		}

		System.out.println();

		// Dump child devices if device is a hub
		if (device.isUsbHub())
		{
			final UsbHub hub = (UsbHub) device;
			for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
			{
				dumpDevice(child);
			}
		}
	}

}
