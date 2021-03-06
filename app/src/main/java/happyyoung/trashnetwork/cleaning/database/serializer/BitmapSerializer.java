package happyyoung.trashnetwork.cleaning.database.serializer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.activeandroid.serializer.TypeSerializer;
import java.io.ByteArrayOutputStream;

/**
 * Created by zhou-shengyun on 7/30/16.
 */

public class BitmapSerializer extends TypeSerializer {
    @Override
    public Class<?> getDeserializedType() {
        return Bitmap.class;
    }

    @Override
    public Class<?> getSerializedType() {
        return Byte[].class;
    }

    @Override
    public Object serialize(Object data) {
        if(data == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ((Bitmap)data).compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    @Override
    public Object deserialize(Object data) {
        if(data == null)
            return null;
        return BitmapFactory.decodeByteArray((byte[])data, 0, ((byte[])data).length);
    }
}
