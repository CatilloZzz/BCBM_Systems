import java.nio.file.*;
import org.mindrot.jbcrypt.BCrypt;

public class PrepararHashes {
    public static void main(String[] args) throws Exception {
        String[][] cuentas = {{"admin", "admin123"}, {"despachador", "despacho123"}, {"bombero1", "bombero123"}};
        StringBuilder salida = new StringBuilder();
        for (String[] cuenta : cuentas) {
            String hash = BCrypt.hashpw(cuenta[1], BCrypt.gensalt(12));
            if (!BCrypt.checkpw(cuenta[1], hash)) throw new IllegalStateException("Hash inválido");
            salida.append(cuenta[0]).append('=').append(hash).append('\n');
        }
        Files.writeString(Path.of(args[0]), salida);
        System.out.println("Tres hashes de demostración generados y verificados con BCrypt.");
    }
}
