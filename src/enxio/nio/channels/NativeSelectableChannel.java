package enxio.nio.channels;

import java.nio.channels.Channel;

public interface NativeSelectableChannel extends Channel {
    
    public int getFD();
}
