package co.codewizards.raspi1;

import java.util.Arrays;
import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbEndpoint;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbPipe;
import javax.usb.UsbPort;
import javax.usb.UsbServices;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;

public class UsbStecaDemo implements Runnable {

	public static final short VENDOR_ID = 0x665;
	public static final short PRODUCT_ID = 0x5161;

	public UsbStecaDemo(String[] args) {
	}

	@Override
	public void run() {
//		final byte[] command = new byte[] {
//				'Q', 'P', 'I', 'G', 'S',
//				(byte) 0xB7, (byte) 0xA9, // CRC
//				'\r'
//		};

		final byte[] command = new byte[] {
				'Q', 'M', 'O', 'D',
				(byte) 0x49, (byte) 0xC1, // CRC
				'\r'
		};

		try {
			UsbDevice device = findSteca();
			System.out.println(device);

			UsbControlIrp irp = device.createUsbControlIrp(
			    (byte) (UsbConst.REQUESTTYPE_DIRECTION_IN
			          | UsbConst.REQUESTTYPE_TYPE_VENDOR
			          | UsbConst.REQUESTTYPE_RECIPIENT_ENDPOINT),
			    UsbConst.REQUEST_GET_STATUS,
			    (short) 0,
			    (short) 0
			    );
			irp.setData(command);
//			device.asyncSubmit(irp);
//			System.out.println(Arrays.toString(irp.getData()));
//			for (byte b : irp.getData()) {
//				System.out.println(((char) (b & 0xff)));
//			}

			UsbConfiguration configuration = device.getActiveUsbConfiguration();

			@SuppressWarnings("unchecked")
			List<UsbInterface> usbInterfaces = configuration.getUsbInterfaces();
			System.out.println("usbInterfaces.size: " + usbInterfaces.size());

			UsbInterface iface = (UsbInterface) configuration.getUsbInterfaces().get(0); //There was only 1

			if (!iface.isClaimed()) {
				iface.claim(usbInterface -> true);
			}
			try {
				@SuppressWarnings("unchecked")
				List<UsbEndpoint> usbEndpoints = iface.getUsbEndpoints();
				System.out.println("usbEndpoints.size: " + usbEndpoints.size());

				UsbEndpoint endpoint = (UsbEndpoint) iface.getUsbEndpoints().get(0);
				UsbPipe pipe = endpoint.getUsbPipe();

				pipe.open();
				try {
					pipe.addUsbPipeListener(new UsbPipeListener() {

						@Override
						public void errorEventOccurred(UsbPipeErrorEvent event) {
							System.out.println("errorEventOccurred: " + event);
							System.out.println(event.getUsbException());
						}

						@Override
						public void dataEventOccurred(UsbPipeDataEvent event) {
							System.out.println("dataEventOccurred: " + event);
							byte[] data = event.getData();
							System.out.println(Arrays.toString(data));
						}
					});

					System.out.println("sending...");
//					UsbIrp irp = pipe.createUsbIrp();
					irp.setData(command);
					device.asyncSubmit(irp);
//					pipe.asyncSubmit(irp);
//					int sent = pipe.syncSubmit(command);
					System.out.println("sent");
//					System.out.println(Arrays.toString(usbIrp.getData()));

					Thread.sleep(10000L);
				} finally {
					pipe.close();
				}
			} finally {
				iface.release();
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	private UsbDevice findSteca() throws Exception {
		final UsbServices services = UsbHostManager.getUsbServices();
		return findSteca(services.getRootUsbHub());
	}

	private UsbDevice findSteca(final UsbDevice device) throws Exception {
		UsbDeviceDescriptor usbDeviceDescriptor = device.getUsbDeviceDescriptor();
		short idVendor = usbDeviceDescriptor.idVendor();
		short idProduct = usbDeviceDescriptor.idProduct();

		if (VENDOR_ID == idVendor && PRODUCT_ID == idProduct) {
			return device;
		}

		if (device.isUsbHub()) {
			final UsbHub hub = (UsbHub) device;
			for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices()) {
				UsbDevice steca = findSteca(child);
				if (steca != null)
					return steca;
			}
		}
		return null;
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
