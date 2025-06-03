package com.liceolapaz.acc;

import com.liceolapaz.acc.DAO.JugadorDAO;
import com.liceolapaz.acc.DAO.PreguntaDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Servidor TCP para el juego de Trivia
 * Maneja conexiones de múltiples clientes y organiza partidas individuales y multijugador
 * Utiliza un sistema de cola para emparejar jugadores de forma eficiente
 */
public class ServidorTrivia {

    // Configuración del servidor
    private static final int PUERTO = 65001; // Puerto TCP para conexiones de clientes
    private static final BlockingQueue<JugadorPendiente> colaClientes = new LinkedBlockingQueue<>(); // Cola thread-safe para jugadores en espera
    private static volatile boolean partidaEnCurso = false; // Flag para controlar si hay una partida activa

    /**
     * Método principal del servidor
     * Inicializa la base de datos, configura el servidor TCP y gestiona conexiones
     */
    public static void main(String[] args) {
        System.out.println("🚀 Iniciando Servidor de Trivia...");

        // Verificar conectividad y contenido de la base de datos
        inicializarBaseDatos();

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("✅ Servidor Trivia iniciado en puerto " + PUERTO);
            System.out.println("📚 Esperando conexiones de jugadores...");

            // Hilo dedicado para procesar la cola de jugadores y organizar partidas
            Thread procesadorCola = new Thread(() -> {
                System.out.println("🔄 Hilo procesador de cola iniciado");
                while (true) {
                    try {
                        if (!partidaEnCurso && !colaClientes.isEmpty()) {
                            System.out.println("🔄 Procesando cola de clientes (" + colaClientes.size() + " en espera)");
                            procesarSiguientePartida();
                        }
                        Thread.sleep(200); // Revisar cada 200ms para mejor responsividad
                    } catch (Exception e) {
                        System.out.println("❌ Error en el gestor de partidas: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            procesadorCola.setDaemon(true); // Hilo daemon que termina con la aplicación
            procesadorCola.start();

            // Bucle principal - acepta conexiones entrantes indefinidamente
            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("🔗 Nueva conexión desde: " + cliente.getInetAddress());
                new Thread(new ManejadorCliente(cliente)).start();
            }

        } catch (IOException e) {
            System.out.println("❌ Error fatal del servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inicializa y verifica la conexión con la base de datos
     * Comprueba que las categorías y preguntas estén disponibles
     */
    private static void inicializarBaseDatos() {
        try {
            System.out.println("🔄 Verificando conexión con la base de datos...");

            // Verificar que la conexión funciona y obtener categorías disponibles
            List<String> categorias = PreguntaDAO.obtenerCategorias();
            System.out.println("📊 Categorías disponibles: " + categorias.size() + " encontradas");

            // Verificar que hay preguntas suficientes para cada categoría
            for (String categoria : categorias) {
                long count = PreguntaDAO.contarPreguntasPorCategoria(categoria);
                System.out.println("  📁 " + categoria + ": " + count + " preguntas");
            }

            System.out.println("✅ Base de datos conectada correctamente");

        } catch (Exception e) {
            System.out.println("⚠️ Error al conectar con la BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clase interna para representar un jugador en espera de partida
     * Contiene toda la información necesaria para gestionar la conexión y preferencias
     */
    private static class JugadorPendiente {
        Socket socket; // Conexión TCP del jugador
        String nombre; // Nombre del jugador
        String categoria; // Categoría de preguntas preferida
        String modo; // Modo de juego: "solo" o "esperar" (multijugador)
        BufferedReader in; // Flujo de entrada para recibir mensajes
        PrintWriter out; // Flujo de salida para enviar mensajes
        long tiempoEspera; // Timestamp de cuando entró en cola
        volatile boolean cancelado = false; // Flag para indicar si el jugador canceló

        /**
         * Constructor para crear un jugador pendiente
         */
        JugadorPendiente(Socket socket, String nombre, String categoria, String modo,
                         BufferedReader in, PrintWriter out) {
            this.socket = socket;
            this.nombre = nombre;
            this.categoria = categoria != null ? categoria.toLowerCase() : "conocimiento-general";
            this.modo = modo.toLowerCase();
            this.in = in;
            this.out = out;
            this.tiempoEspera = System.currentTimeMillis();
        }

        /**
         * Verifica si el jugador sigue siendo válido para participar en una partida
         * @return true si el jugador está conectado y no ha cancelado
         */
        boolean esValido() {
            boolean socketValido = socket != null && !socket.isClosed() && !cancelado;
            boolean nombreValido = nombre != null && !nombre.trim().isEmpty();
            boolean resultado = socketValido && nombreValido;

            if (!resultado) {
                System.out.println("⚠️ Jugador inválido: " + nombre +
                        " (socket: " + socketValido + ", nombre: " + nombreValido + ", cancelado: " + cancelado + ")");
            }

            return resultado;
        }

        /**
         * Calcula el tiempo que lleva esperando en cola
         * @return Tiempo en milisegundos desde que entró en cola
         */
        long tiempoEsperando() {
            return System.currentTimeMillis() - tiempoEspera;
        }

        /**
         * Marca al jugador como cancelado para exclusión de la cola
         */
        void marcarCancelado() {
            this.cancelado = true;
        }
    }

    /**
     * Clase que maneja la comunicación con un cliente individual
     * Se ejecuta en un hilo separado para cada conexión entrante
     */
    private static class ManejadorCliente implements Runnable {
        private final Socket socket; // Socket de la conexión del cliente

        /**
         * Constructor del manejador de cliente
         */
        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        /**
         * Método principal que maneja todo el flujo de comunicación con el cliente
         */
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                System.out.println("👋 Cliente conectado desde: " + socket.getInetAddress());

                // Paso 1: Solicitar y validar nombre del jugador
                String nombre = solicitarNombre(in, out);
                if (nombre == null) return;

                // Paso 2: Seleccionar categoría y modo de juego
                String[] seleccion = solicitarCategoriaYModo(in, out, nombre);
                if (seleccion == null) return;

                String categoria = seleccion[0];
                String modo = seleccion[1];

                // Paso 3: Verificar si hay partida en curso para multijugador
                synchronized (ServidorTrivia.class) {
                    if (partidaEnCurso && "esperar".equals(modo)) {
                        out.println("PARTIDA_EN_CURSO;MENSAJE:Hay una partida multijugador en curso. Espera o juega solo.");
                        System.out.println("⚠️ " + nombre + " rechazado - partida en curso");
                        return;
                    }
                }

                // Paso 4: Crear jugador pendiente y añadir a la cola de espera
                JugadorPendiente jugador = new JugadorPendiente(socket, nombre, categoria, modo, in, out);

                // Iniciar hilo para escuchar cancelaciones mientras espera
                iniciarEscuchaCancelacion(jugador);

                colaClientes.put(jugador);
                System.out.println("👤 " + nombre + " agregado a la cola (" + modo + ", " + categoria + ")");
                System.out.println("📊 Total en cola: " + colaClientes.size());

                // IMPORTANTE: Mantener la conexión abierta para la partida
                // La conexión se cerrará cuando termine la partida

                // Mantener el hilo vivo hasta que la partida termine o se desconecte
                while (!socket.isClosed() && jugador.esValido()) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                System.out.println("❌ Error manejando cliente: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Cerrar conexión solo si no está en una partida activa
                try {
                    if (socket != null && !socket.isClosed()) {
                        System.out.println("🔌 Cerrando conexión del cliente");
                        socket.close();
                    }
                } catch (IOException ignored) {}
            }
        }

        /**
         * Solicita y valida el nombre del jugador
         * @return Nombre válido del jugador o null si se cancela
         */
        private String solicitarNombre(BufferedReader in, PrintWriter out) throws IOException {
            out.println("SOLICITUD_NOMBRE");
            String nombre = in.readLine();

            if (nombre == null || nombre.trim().isEmpty() || "cancelar".equalsIgnoreCase(nombre)) {
                out.println("CONEXION_CANCELADA");
                return null;
            }

            nombre = nombre.trim();
            JugadorDAO.verificarYCrearJugador(nombre); // Crear jugador en BD si no existe
            System.out.println("✅ Jugador identificado: " + nombre);
            return nombre;
        }

        /**
         * Solicita la categoría y modo de juego al cliente
         * También maneja comandos especiales como estadísticas y puntuación
         * @return Array con [categoría, modo] o null si se cancela
         */
        private String[] solicitarCategoriaYModo(BufferedReader in, PrintWriter out, String nombre) throws IOException {
            List<String> categoriasDisponibles = List.of("conocimiento-general", "musica", "geografia", "deportes");

            while (true) {
                // Enviar lista de categorías disponibles
                StringBuilder categoriasMsg = new StringBuilder("CATEGORIAS_DISPONIBLES");
                for (String cat : categoriasDisponibles) {
                    categoriasMsg.append(";").append(cat);
                }
                out.println(categoriasMsg.toString());

                String respuesta = in.readLine();
                if (respuesta == null || "cancelar".equalsIgnoreCase(respuesta)) {
                    out.println("CONEXION_CANCELADA");
                    return null;
                }

                // Procesar comandos especiales del cliente
                if ("estadisticas".equalsIgnoreCase(respuesta)) {
                    String stats = JugadorDAO.obtenerEstadisticas(nombre);
                    out.println("ESTADISTICAS;" + stats.replace("\n", "|"));
                    continue;
                }

                if ("puntuacion".equalsIgnoreCase(respuesta)) {
                    int puntos = JugadorDAO.obtenerPuntuacion(nombre);
                    out.println("PUNTUACION_TOTAL;" + puntos);
                    continue;
                }

                // Parsear selección en formato: "categoria:modo" (ej: "musica:solo")
                String[] partes = respuesta.split(":");
                if (partes.length != 2) {
                    out.println("SELECCION_INVALIDA;FORMATO:categoria:modo");
                    continue;
                }

                String categoria = partes[0].toLowerCase().trim();
                String modo = partes[1].toLowerCase().trim();

                // Validar categoría seleccionada
                if (!categoriasDisponibles.contains(categoria)) {
                    out.println("CATEGORIA_INVALIDA;" + categoria);
                    continue;
                }

                // Validar modo de juego
                if (!"solo".equals(modo) && !"esperar".equals(modo)) {
                    out.println("MODO_INVALIDO;" + modo);
                    continue;
                }

                System.out.println("🎯 " + nombre + " eligió: " + categoria + " (" + modo + ")");
                return new String[]{categoria, modo};
            }
        }

        /**
         * Inicia un hilo para escuchar cancelaciones del cliente mientras espera en cola
         * @param jugador El jugador pendiente a monitorear
         */
        private void iniciarEscuchaCancelacion(JugadorPendiente jugador) {
            new Thread(() -> {
                try {
                    String linea;
                    while ((linea = jugador.in.readLine()) != null) {
                        if ("cancelar".equalsIgnoreCase(linea)) {
                            System.out.println("❌ " + jugador.nombre + " canceló su espera");
                            jugador.marcarCancelado();
                            colaClientes.remove(jugador);
                            jugador.out.println("CONEXION_CANCELADA");
                            if (!jugador.socket.isClosed()) {
                                jugador.socket.close();
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Cliente desconectado inesperadamente
                    jugador.marcarCancelado();
                    colaClientes.remove(jugador);
                    System.out.println("🔌 " + jugador.nombre + " se desconectó");
                }
            }).start();
        }
    }

    /**
     * Procesa la cola de jugadores y organiza partidas según disponibilidad y preferencias
     * Prioriza partidas individuales sobre multijugador para mejor experiencia
     */
    private static void procesarSiguientePartida() {
        synchronized (ServidorTrivia.class) {
            try {
                // Limpiar clientes desconectados primero
                int clientesAntes = colaClientes.size();
                colaClientes.removeIf(j -> !j.esValido());
                int clientesDespues = colaClientes.size();

                if (clientesAntes != clientesDespues) {
                    System.out.println("🧹 Limpieza: " + (clientesAntes - clientesDespues) + " clientes desconectados eliminados");
                }

                if (colaClientes.isEmpty()) {
                    return;
                }

                System.out.println("🔍 Procesando cola: " + colaClientes.size() + " jugadores");

                // Prioridad 1: Jugadores individuales (inicio inmediato)
                JugadorPendiente jugadorSolo = encontrarJugadorSolo();
                if (jugadorSolo != null) {
                    iniciarPartidaSolo(jugadorSolo);
                    return;
                }

                // Prioridad 2: Parejas multijugador
                JugadorPendiente[] pareja = encontrarParejaMultijugador();
                if (pareja != null) {
                    iniciarPartidaMultijugador(pareja[0], pareja[1]);
                }

            } catch (Exception e) {
                System.out.println("❌ Error procesando partidas: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Busca un jugador que quiera jugar en modo individual
     * @return Jugador encontrado o null si no hay ninguno
     */
    private static JugadorPendiente encontrarJugadorSolo() {
        for (JugadorPendiente jugador : colaClientes) {
            System.out.println("🔍 Revisando jugador: " + jugador.nombre +
                    " (modo: " + jugador.modo + ", válido: " + jugador.esValido() + ")");
            if ("solo".equals(jugador.modo) && jugador.esValido()) {
                colaClientes.remove(jugador);
                System.out.println("✅ Jugador solo encontrado: " + jugador.nombre);
                return jugador;
            }
        }
        System.out.println("❌ No se encontró ningún jugador solo válido");
        return null;
    }

    /**
     * Busca una pareja compatible para partida multijugador
     * Prioriza emparejar por misma categoría, luego por tiempo de espera
     * @return Array con dos jugadores emparejados o null si no es posible
     */
    private static JugadorPendiente[] encontrarParejaMultijugador() {
        List<JugadorPendiente> esperando = new ArrayList<>();

        // Recopilar todos los jugadores esperando multijugador
        for (JugadorPendiente jugador : colaClientes) {
            if ("esperar".equals(jugador.modo) && jugador.esValido()) {
                esperando.add(jugador);
            }
        }

        System.out.println("👥 Jugadores esperando multijugador: " + esperando.size());

        if (esperando.size() < 2) {
            return null;
        }

        // Intentar emparejar por misma categoría primero
        for (int i = 0; i < esperando.size(); i++) {
            for (int j = i + 1; j < esperando.size(); j++) {
                JugadorPendiente j1 = esperando.get(i);
                JugadorPendiente j2 = esperando.get(j);

                if (j1.categoria.equals(j2.categoria)) {
                    colaClientes.remove(j1);
                    colaClientes.remove(j2);
                    System.out.println("✅ Pareja encontrada (misma categoría): " + j1.nombre + " + " + j2.nombre + " (" + j1.categoria + ")");
                    return new JugadorPendiente[]{j1, j2};
                }
            }
        }

        // Si no hay coincidencia de categoría, emparejar cualquier dos si han esperado suficiente tiempo
        long tiempoLimite = 10000; // 10 segundos
        for (int i = 0; i < esperando.size(); i++) {
            for (int j = i + 1; j < esperando.size(); j++) {
                JugadorPendiente j1 = esperando.get(i);
                JugadorPendiente j2 = esperando.get(j);

                if (j1.tiempoEsperando() > tiempoLimite || j2.tiempoEsperando() > tiempoLimite) {
                    colaClientes.remove(j1);
                    colaClientes.remove(j2);
                    // Usar la categoría del jugador que más tiempo ha esperado
                    String categoriaFinal = j1.tiempoEsperando() > j2.tiempoEsperando() ? j1.categoria : j2.categoria;
                    j1.categoria = categoriaFinal;
                    j2.categoria = categoriaFinal;
                    System.out.println("✅ Pareja encontrada (tiempo límite): " + j1.nombre + " + " + j2.nombre + " (" + categoriaFinal + ")");
                    return new JugadorPendiente[]{j1, j2};
                }
            }
        }

        System.out.println("❌ No se pudo formar pareja multijugador");
        return null;
    }

    /**
     * Inicia una partida individual para un jugador
     * @param jugador El jugador que participará en modo individual
     */
    private static void iniciarPartidaSolo(JugadorPendiente jugador) {
        partidaEnCurso = true;
        System.out.println("🎮 Iniciando partida individual: " + jugador.nombre + " (" + jugador.categoria + ")");

        // Notificar al cliente que se encontró la partida
        jugador.out.println("PARTIDA_ENCONTRADA;TIPO:SOLO;CATEGORIA:" + jugador.categoria);

        // Pausa para que el cliente procese el mensaje
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Ejecutar partida en hilo separado
        new Thread(() -> {
            try {
                new PartidaTrivia(jugador.socket, null, jugador.nombre, null, jugador.categoria).run();
            } catch (Exception e) {
                System.out.println("❌ Error en partida individual: " + e.getMessage());
                e.printStackTrace();
            } finally {
                partidaEnCurso = false;
                System.out.println("✅ Partida individual finalizada");
            }
        }).start();
    }

    /**
     * Inicia una partida multijugador entre dos jugadores
     * @param j1 Primer jugador
     * @param j2 Segundo jugador
     */
    private static void iniciarPartidaMultijugador(JugadorPendiente j1, JugadorPendiente j2) {
        partidaEnCurso = true;
        System.out.println("🆚 Iniciando partida multijugador: " + j1.nombre + " vs " + j2.nombre +
                " (" + j1.categoria + ")");

        // Notificar a ambos clientes sobre la partida encontrada
        j1.out.println("PARTIDA_ENCONTRADA;TIPO:MULTIJUGADOR;OPONENTE:" + j2.nombre + ";CATEGORIA:" + j1.categoria);
        j2.out.println("PARTIDA_ENCONTRADA;TIPO:MULTIJUGADOR;OPONENTE:" + j1.nombre + ";CATEGORIA:" + j1.categoria);

        // Pausa para que ambos clientes procesen el mensaje
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Ejecutar partida multijugador en hilo separado
        new Thread(() -> {
            try {
                new PartidaTrivia(j1.socket, j2.socket, j1.nombre, j2.nombre, j1.categoria).run();
            } catch (Exception e) {
                System.out.println("❌ Error en partida multijugador: " + e.getMessage());
                e.printStackTrace();
            } finally {
                partidaEnCurso = false;
                System.out.println("✅ Partida multijugador finalizada");
            }
        }).start();
    }
}