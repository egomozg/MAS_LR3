package ru.mpei;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.Optional;

public class XmlSerialization {

    public static <T> Optional<T> deserialize(String path, Class<T> clazz){
        File f = new File(path);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            T cfg = (T) unmarshaller.unmarshal(f);
            return Optional.ofNullable(cfg);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean serialize(String path, Object o){
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(o.getClass());
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(o, new File(path));
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

}

