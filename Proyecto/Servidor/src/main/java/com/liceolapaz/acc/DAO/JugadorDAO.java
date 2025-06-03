package com.liceolapaz.acc.DAO;

import com.liceolapaz.acc.entidades.Jugador;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class JugadorDAO {

    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    public static void verificarYCrearJugador(String nombre) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar si ya existe el jugador por nombre
            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador == null) {
                // Si no existe, lo creamos
                jugador = new Jugador();
                jugador.setNombre(nombre);
                session.persist(jugador);
                System.out.println("✅ Jugador nuevo añadido: " + nombre);
            } else {
                System.out.println("👤 Jugador ya existe: " + nombre + " (ID: " + jugador.getId() + ")");
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al verificar o crear jugador: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    public static Jugador obtenerJugador(String nombre) {
        Session session = sf.openSession();
        try {
            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador != null) {
                System.out.println("🔍 Jugador encontrado: " + jugador.getNombre() + " (Puntos: " + jugador.getPuntuacionTotal() + ")");
            }

            return jugador;
        } catch (Exception e) {
            System.out.println("❌ Error al obtener jugador: " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    // FIXED: Método de actualización de puntuación corregido con mejor manejo de transacciones
    public static void actualizarPuntuacionJugador(String nombre, int puntos) {
        Session session = sf.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador != null) {
                int puntuacionAnterior = jugador.getPuntuacionTotal();
                int nuevaPuntuacion = puntuacionAnterior + puntos;

                jugador.setPuntuacionTotal(nuevaPuntuacion);
                session.merge(jugador);

                // FIXED: Commit ANTES de logging para evitar problemas
                tx.commit();
                tx = null; // Marcar como completado

                System.out.println("⬆️ Puntuación actualizada para " + nombre + ": " +
                        puntuacionAnterior + " + " + puntos + " = " + nuevaPuntuacion);
            } else {
                System.out.println("❌ Jugador no encontrado para actualizar puntuación: " + nombre);
                if (tx != null) {
                    tx.rollback();
                    tx = null;
                }
            }

        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    System.out.println("❌ Error en rollback: " + rollbackEx.getMessage());
                }
            }
            System.out.println("❌ Error al actualizar la puntuación del jugador " + nombre + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public static int obtenerPuntuacion(String nombre) {
        Session session = sf.openSession();
        try {
            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador != null) {
                System.out.println("📊 Puntuación de " + nombre + ": " + jugador.getPuntuacionTotal() + " puntos");
                return jugador.getPuntuacionTotal();
            } else {
                System.out.println("❌ Jugador no encontrado: " + nombre);
                return 0;
            }

        } catch (Exception e) {
            System.out.println("❌ Error al obtener puntuación: " + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    // FIXED: Método corregido con mejor manejo de transacciones
    public static void incrementarPartidasJugadas(String nombre) {
        Session session = sf.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador != null) {
                jugador.incrementarPartidaJugada();
                session.merge(jugador);

                tx.commit();
                tx = null;

                System.out.println("🎮 Partidas jugadas de " + nombre + ": " + jugador.getPartidasJugadas());
            } else {
                System.out.println("❌ Jugador no encontrado para incrementar partidas: " + nombre);
                if (tx != null) {
                    tx.rollback();
                    tx = null;
                }
            }

        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    System.out.println("❌ Error en rollback: " + rollbackEx.getMessage());
                }
            }
            System.out.println("❌ Error al incrementar partidas jugadas: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    // FIXED: Método corregido con mejor manejo de transacciones
    public static void incrementarPartidasGanadas(String nombre) {
        Session session = sf.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            Jugador jugador = session.createQuery(
                            "FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();

            if (jugador != null) {
                jugador.incrementarPartidaGanada();
                session.merge(jugador);

                tx.commit();
                tx = null;

                System.out.println("🏆 Partidas ganadas de " + nombre + ": " + jugador.getPartidasGanadas());
            } else {
                System.out.println("❌ Jugador no encontrado para incrementar victorias: " + nombre);
                if (tx != null) {
                    tx.rollback();
                    tx = null;
                }
            }

        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    System.out.println("❌ Error en rollback: " + rollbackEx.getMessage());
                }
            }
            System.out.println("❌ Error al incrementar partidas ganadas: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    public static String obtenerEstadisticas(String nombre) {
        Jugador jugador = obtenerJugador(nombre);
        if (jugador != null) {
            return String.format(
                    "📊 Estadísticas de %s:\n" +
                            "💰 Puntos totales: %d\n" +
                            "🎮 Partidas jugadas: %d\n" +
                            "🏆 Partidas ganadas: %d\n" +
                            "📈 Porcentaje de victorias: %.1f%%",
                    jugador.getNombre(),
                    jugador.getPuntuacionTotal(),
                    jugador.getPartidasJugadas(),
                    jugador.getPartidasGanadas(),
                    jugador.getPorcentajeVictorias()
            );
        }
        return "❌ Jugador no encontrado: " + nombre;
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