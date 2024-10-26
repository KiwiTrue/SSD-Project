import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class GetMacAddress {
    
    // Method to get the MAC address of the device
    public static String getMacAddress() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            
            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return "MAC Address not available";
            }
    
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (UnknownHostException e) {
            return "Host name could not be resolved";
        } catch (SocketException e) {
            e.printStackTrace();
            return "Error retrieving MAC Address";
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "No network interfaces found";
        }
    }
}