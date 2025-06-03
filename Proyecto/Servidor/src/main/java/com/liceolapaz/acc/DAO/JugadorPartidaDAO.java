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

    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    /**
     * Registers a player's participation in a game with correct answers and points
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
            jugadorPartida.setPuntosObtenidos(puntos); // Use provided points, don't recalculate

            session.persist(jugadorPartida);
            tx.commit();

            System.out.println("✅ Registro JugadorPartida: " +
                    jugador.getNombre() + " - " + respuestasCorrectas + " aciertos - " + puntos + " puntos");

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al registrar JugadorPartida: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * Registers a player's participation with automatic point calculation (deprecated - use the version above)
     */
    public static void registrarJugadorPartida(Jugador jugador, Partida partida, int respuestasCorrectas) {
        int puntosCalculados = calcularPuntos(respuestasCorrectas, partida.getTotalPreguntas());
        registrarJugadorPartida(jugador, partida, respuestasCorrectas, puntosCalculados);
    }

    public static void marcarComoGanador(Jugador jugador, Partida partida) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
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

    public static void establecerTiempoJugador(Jugador jugador, Partida partida, long tiempoSegundos) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
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

    public static List<JugadorPartida> obtenerHistorialJugador(String nombreJugador) {
        Session session = sf.openSession();
        try {
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

    public static List<JugadorPartida> obtenerMejoresJugadores(int limite) {
        Session session = sf.openSession();
        try {
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

    public static String obtenerEstadisticasRendimiento(String nombreJugador) {
        Session session = sf.openSession();
        try {
            Double promedioAciertos = (Double) session.createQuery(
                            "SELECT AVG(jp.respuestasCorrectas) FROM JugadorPartida jp WHERE jp.jugador.nombre = :nombre")
                    .setParameter("nombre", nombreJugador)
                    .uniqueResult();

            Integer mejorPuntuacion = (Integer) session.createQuery(
                            "SELECT MAX(jp.puntosObtenidos) FROM JugadorPartida jp WHERE jp.jugador.nombre = :nombre")
                    .setParameter("nombre", nombreJugador)
                    .uniqueResult();

            Long victorias = (Long) session.createQuery(
                            "SELECT COUNT(jp) FROM JugadorPartida jp WHERE jp.jugador.nombre = :nombre AND jp.ganador = true")
                    .setParameter("nombre", nombreJugador)
                    .uniqueResult();

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
     * Calculate points based on correct answers (used only as fallback)
     */
    private static int calcularPuntos(int respuestasCorrectas, int totalPreguntas) {
        double porcentaje = (double) respuestasCorrectas / totalPreguntas;

        if (porcentaje >= 0.9) return 5;
        else if (porcentaje >= 0.7) return 3;
        else if (porcentaje >= 0.5) return 1;
        else return 0;
    }

    public static void cerrarFactory() {
        if (sf != null) {
            sf.close();
        }
        if (sr != null) {
            StandardServiceRegistryBuilder.destroy(sr);
        }
    }
}
