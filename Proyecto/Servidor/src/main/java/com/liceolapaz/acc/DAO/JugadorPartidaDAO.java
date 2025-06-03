package com.liceolapaz.acc.DAO;

import com.liceolapaz.acc.entidades.Jugador;
import com.liceolapaz.acc.entidades.JugadorPartida;
import com.liceolapaz.acc.entidades.Partida;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.List;

public class JugadorPartidaDAO {

    // Registro de servicios estándar de Hibernate configurado desde hibernate.cfg.xml
    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    // Fábrica de sesiones de Hibernate para gestionar conexiones a la base de datos
    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    /**
     * Registra la participación de un jugador en una partida con respuestas correctas y puntos
     * @param jugador El jugador que participó
     * @param partida La partida en la que participó
     * @param respuestasCorrectas Número de respuestas correctas
     * @param puntos Puntos obtenidos por el jugador
     */
    public static void registrarJugadorPartida(Jugador jugador, Partida partida, int respuestasCorrectas, int puntos) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            JugadorPartida jugadorPartida = new JugadorPartida();
            jugadorPartida.setJugador(jugador);
            jugadorPartida.setPartida(partida);
            jugadorPartida.setRespuestasCorrectas(respuestasCorrectas);
            jugadorPartida.setRespuestasIncorrectas(partida.getTotalPreguntas() - respuestasCorrectas);
            jugadorPartida.setPuntosObtenidos(puntos); // Usar puntos proporcionados, no recalcular

            session.persist(jugadorPartida);
            tx.commit();

            System.out.println("✅ Registro JugadorPartida: " +
                    jugador.getNombre() + " - " + respuestasCorrectas + " aciertos - " + puntos + " puntos");

        } catch (Exception e) {
            // Revertir transacción en caso de error
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al registrar JugadorPartida: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * Registra la participación de un jugador con cálculo automático de puntos (obsoleto - usar versión anterior)
     * @param jugador El jugador que participó
     * @param partida La partida en la que participó
     * @param respuestasCorrectas Número de respuestas correctas
     */
    public static void registrarJugadorPartida(Jugador jugador, Partida partida, int respuestasCorrectas) {
        int puntosCalculados = calcularPuntos(respuestasCorrectas, partida.getTotalPreguntas());
        registrarJugadorPartida(jugador, partida, respuestasCorrectas, puntosCalculados);
    }

    /**
     * Marca a un jugador como ganador de una partida específica
     * @param jugador El jugador ganador
     * @param partida La partida ganada
     */
    public static void marcarComoGanador(Jugador jugador, Partida partida) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar el registro JugadorPartida específico
            JugadorPartida jugadorPartida = session.createQuery(
                            "FROM JugadorPartida jp WHERE jp.jugador = :jugador AND jp.partida = :partida",
                            JugadorPartida.class)
                    .setParameter("jugador", jugador)
                    .setParameter("partida", partida)
                    .uniqueResult();

            if (jugadorPartida != null) {
                jugadorPartida.setGanador(true);
                jugadorPartida.setPosicion(1);
                session.merge(jugadorPartida);
                tx.commit();

                System.out.println("🏆 " + jugador.getNombre() + " marcado como ganador de la partida " + partida.getId());
            } else {
                tx.rollback();
            }

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al marcar ganador: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    /**
     * Establece el tiempo total empleado por un jugador en una partida
     * @param jugador El jugador
     * @param partida La partida
     * @param tiempoSegundos Tiempo en segundos
     */
    public static void establecerTiempoJugador(Jugador jugador, Partida partida, long tiempoSegundos) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar el registro JugadorPartida específico
            JugadorPartida jugadorPartida = session.createQuery(
                            "FROM JugadorPartida jp WHERE jp.jugador = :jugador AND jp.partida = :partida",
                            JugadorPartida.class)
                    .setParameter("jugador", jugador)
                    .setParameter("partida", partida)
                    .uniqueResult();

            if (jugadorPartida != null) {
                jugadorPartida.setTiempoTotalSegundos(tiempoSegundos);
                session.merge(jugadorPartida);
                tx.commit();

                System.out.println("⏱️ Tiempo registrado para " + jugador.getNombre() + ": " +
                        jugadorPartida.getTiempoFormateado());
            } else {
                tx.rollback();
            }

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al establecer tiempo: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    /**
     * Obtiene el historial de partidas de un jugador específico
     * @param nombreJugador Nombre del jugador
     * @return Lista de registros JugadorPartida ordenados por fecha descendente
     */
    public static List<JugadorPartida> obtenerHistorialJugador(String nombreJugador) {
        Session session = sf.openSession();
        try {
            // Consulta con JOIN FETCH para cargar datos de partida de forma eficiente
            List<JugadorPartida> historial = session.createQuery(
                            "FROM JugadorPartida jp JOIN FETCH jp.partida WHERE jp.jugador.nombre = :nombre ORDER BY jp.partida.fechaHora DESC",
                            JugadorPartida.class)
                    .setParameter("nombre", nombreJugador)
                    .getResultList();

            System.out.println("📋 Historial de " + nombreJugador + ": " + historial.size() + " partidas");
            return historial;

        } catch (Exception e) {
            System.out.println("❌ Error al obtener historial: " + e.getMessage());
            return List.of();
        } finally {
            session.close();
        }
    }

    /**
     * Obtiene los mejores jugadores basado en puntuaciones más altas
     * @param limite Número máximo de resultados a devolver
     * @return Lista de los mejores registros JugadorPartida ordenados por puntos
     */
    public static List<JugadorPartida> obtenerMejoresJugadores(int limite) {
        Session session = sf.openSession();
        try {
            // Consulta con JOIN FETCH para cargar datos de jugador de forma eficiente
            List<JugadorPartida> mejores = session.createQuery(
                            "FROM JugadorPartida jp JOIN FETCH jp.jugador ORDER BY jp.puntosObtenidos DESC",
                            JugadorPartida.class)
                    .setMaxResults(limite)
                    .getResultList();

            System.out.println("🏆 Top " + limite + " mejores puntuaciones obtenidas");
            return mejores;

        } catch (Exception e) {
            System.out.println("❌ Error al obtener mejores jugadores: " + e.getMessage());
            return List.of();
        } finally {
            session.close();
        }
    }

    /**
     * Obtiene estadísticas detalladas de rendimiento de un jugador
     * @param nombreJugador Nombre del jugador
     * @return String formateado con estadísticas completas de rendimiento
     */
    public static String obtenerEstadisticasRendimiento(String nombreJugador) {
        Session session = sf.openSession();
        try {
            // Calcular promedio de respuestas correctas
            Double promedioAciertos = (Double) session.createQuery(
                            "SELECT AVG(jp.respuestasCorrectas) FROM JugadorPartida jp WHERE jp.jugador.nombre = :nombre")
                    .setParameter("nombre", nombreJugador)
                    .uniqueResult();

            // Obtener mejor puntuación alcanzada
            Integer mejorPuntuacion = (Integer) session.createQuery(
                            "SELECT MAX(jp.puntosObtenidos) FROM JugadorPartida jp WHERE jp.jugador.nombre = :nombre")
                    .setParameter("nombre", nombreJugador)
                    .uniqueResult();

            // Contar número de victorias
            Long victorias = (Long) session.createQuery(
                            "SELECT COUNT(jp) FROM JugadorPartida jp WHERE jp.jugador.nombre = :nombre AND jp.ganador = true")
                    .setParameter("nombre", nombreJugador)
                    .uniqueResult();

            // Contar total de partidas jugadas
            Long totalPartidas = (Long) session.createQuery(
                            "SELECT COUNT(jp) FROM JugadorPartida jp WHERE jp.jugador.nombre = :nombre")
                    .setParameter("nombre", nombreJugador)
                    .uniqueResult();

            return String.format(
                    "📊 Rendimiento de %s:\n" +
                            "🎯 Promedio de aciertos: %.1f\n" +
                            "🏆 Mejor puntuación: %d\n" +
                            "✅ Victorias: %d\n" +
                            "🎮 Total partidas: %d\n" +
                            "📈 Tasa de victoria: %.1f%%",
                    nombreJugador,
                    promedioAciertos != null ? promedioAciertos : 0.0,
                    mejorPuntuacion != null ? mejorPuntuacion : 0,
                    victorias != null ? victorias : 0,
                    totalPartidas != null ? totalPartidas : 0,
                    (totalPartidas != null && totalPartidas > 0 && victorias != null) ?
                            (victorias * 100.0 / totalPartidas) : 0.0
            );

        } catch (Exception e) {
            System.out.println("❌ Error al obtener estadísticas de rendimiento: " + e.getMessage());
            return "❌ Error al obtener estadísticas";
        } finally {
            session.close();
        }
    }

    /**
     * Calcula puntos basado en respuestas correctas (usado solo como respaldo)
     * @param respuestasCorrectas Número de respuestas correctas
     * @param totalPreguntas Total de preguntas en la partida
     * @return Puntos calculados según porcentaje de acierto
     */
    private static int calcularPuntos(int respuestasCorrectas, int totalPreguntas) {
        double porcentaje = (double) respuestasCorrectas / totalPreguntas;

        // Sistema de puntuación basado en porcentaje de aciertos
        if (porcentaje >= 0.9) return 5;        // 90% o más: 5 puntos
        else if (porcentaje >= 0.7) return 3;   // 70-89%: 3 puntos
        else if (porcentaje >= 0.5) return 1;   // 50-69%: 1 punto
        else return 0;                          // Menos del 50%: 0 puntos
    }

    /**
     * Cierra la SessionFactory y libera recursos de Hibernate
     */
    public static void cerrarFactory() {
        if (sf != null) {
            sf.close();
        }
        if (sr != null) {
            StandardServiceRegistryBuilder.destroy(sr);
        }
    }
}