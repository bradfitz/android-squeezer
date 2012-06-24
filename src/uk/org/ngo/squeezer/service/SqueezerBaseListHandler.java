package uk.org.ngo.squeezer.service;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.ReflectUtil;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import android.support.v4.app.Fragment.InstantiationException;

abstract class SqueezerBaseListHandler<T extends SqueezerItem> implements SqueezerListHandler<T> {
    protected List<T> items;
    @SuppressWarnings("unchecked")
    private Class<T> dataType = (Class<T>) ReflectUtil.getGenericClass(this.getClass(), SqueezerListHandler.class, 0);
    private Constructor<T> constructor;


    public Class<T> getDataType() {
        return dataType;
    }

    public void clear() {
        items = new ArrayList<T>(){private static final long serialVersionUID = 1321113152942485275L;};
    }

    public void add(Map<String, String> record) {
        if (constructor == null)
            try {
                constructor = dataType.getDeclaredConstructor(Map.class);
            } catch (Exception e) {
                throw new InstantiationException("Unable to create constructor for " + dataType.getName(), e);
            }
        try {
            items.add(constructor.newInstance(record));
        } catch (Exception e) {
            throw new InstantiationException("Unable to create new " + dataType.getName(), e);
        }
    }

}
