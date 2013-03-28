package com.stillpig.babyonthecloud.customcode;

import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.jar.JarEntryObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App extends JarEntryObject {

    @Override
    public List<CustomCodeMethod> methods() {
        List<CustomCodeMethod> list = new ArrayList<CustomCodeMethod>();
        list.add(new MassObjectEditor());
        return list;
    }
}
