package utils;

import java.io.*;

public class Serializer {

    // Sauvegarder un objet dans un fichier
    public static void saveObject(Object obj, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
        }
    }

    // Charger un objet depuis un fichier
    public static Object loadObject(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return ois.readObject();
        }
    }
}
