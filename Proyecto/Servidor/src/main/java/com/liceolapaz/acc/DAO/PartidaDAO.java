package com.liceolapaz.acc.DAO;

import com.liceolapaz.acc.entidades.Partida;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class PartidaDAO {

    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    public static Partida registrarPartida(String categoria, boolean completada, String tipoPartida) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            Partida partida = new Partida();
            partida.setCategoria(categoria.toLowerCase());
            partida.setCompletada(completada);
            partida.setTipoPartida(tipoPartida.toUpperCase());
            partida.setTotalPreguntas(10);

            session.persist(partida);
            tx.commit();

            System.out.println("✅ Partida registrada - ID: " + partida.getId() +
                    ", Categoría: " + categoria +
                    ", Tipo: " + tipoPartida +
                    ", Completada: " + completada);

            return partida;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al registrar la partida: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    public static Partida registrarPartida(String categoria, boolean completada) {
        return registrarPartida(categoria, completada, "SOLO");
    }

    public static void marcarPartidaComoCompletada(int partidaId) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            Partida partida = session.get(Partida.class, partidaId);
            if (partida != null) {
                partida.setCompletada(true);
                session.merge(partida);
                tx.commit();

                System.out.println("✅ Partida " + partidaId + " marcada como completada");
            } else {
                System.out.println("❌ Partida no encontrada: " + partidaId);
                tx.rollback();
            }

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al marcar partida como completada: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    public static void establecerDuracionPartida(int partidaId, long duracionSegundos) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            Partida partida = session.get(Partida.class, partidaId);
            if (partida != null) {
                partida.setDuracionSegundos(duracionSegundos);
                session.merge(partida);
                tx.commit();

                System.out.println("⏱️ Duración establecida para partida " + partidaId + ": " +
                        partida.getDuracionFormateada());
            } else {
                System.out.println("❌ Partida no encontrada: " + partidaId);
                tx.rollback();
            }

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al establecer duración: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    public static Partida obtenerPartidaPorId(int partidaId) {
        Session session = sf.openSession();
        try {
            Partida partida = session.get(Partida.class, partidaId);
            if (partida != null) {
                System.out.println("🔍 Partida encontrada: " + partida);
            }
            return partida;
        } catch (Exception e) {
            System.out.println("❌ Error al obtener partida: " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public static long contarPartidasPorCategoria(String categoria) {
        Session session = sf.openSession();
        try {
            Long count = (Long) session.createQuery(
                            "SELECT COUNT(p) FROM Partida p WHERE p.categoria = :categoria")
                    .setParameter("categoria", categoria.toLowerCase())
                    .uniqueResult();

            System.out.println("📊 Partidas jugadas en " + categoria + ": " + count);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.out.println("❌ Error al contar partidas: " + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    public static long contarPartidasCompletadas() {
        Session session = sf.openSession();
        try {
            Long count = (Long) session.createQuery(
                            "SELECT COUNT(p) FROM Partida p WHERE p.completada = true")
                    .uniqueResult();

            System.out.println("📊 Total de partidas completadas: " + count);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.out.println("❌ Error al contar partidas completadas: " + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    public static String obtenerEstadisticasPartidas() {
        Session session = sf.openSession();
        try {
            Long totalPartidas = (Long) session.createQuery(
                    "SELECT COUNT(p) FROM Partida p").uniqueResult();

            Long partidasCompletadas = (Long) session.createQuery(
                    "SELECT COUNT(p) FROM Partida p WHERE p.completada = true").uniqueResult();

            Long partidasSolo = (Long) session.createQuery(
                    "SELECT COUNT(p) FROM Partida p WHERE p.tipoPartida = 'SOLO'").uniqueResult();

            Long partidasMulti = (Long) session.createQuery(
                    "SELECT COUNT(p) FROM Partida p WHERE p.tipoPartida = 'MULTIJUGADOR'").uniqueResult();

            return String.format(
                    "📊 Estadísticas de Partidas:\n" +
                            "🎮 Total de partidas: %d\n" +
                            "✅ Partidas completadas: %d\n" +
                            "👤 Partidas individuales: %d\n" +
                            "👥 Partidas multijugador: %d",
                    totalPartidas != null ? totalPartidas : 0,
                    partidasCompletadas != null ? partidasCompletadas : 0,
                    partidasSolo != null ? partidasSolo : 0,
                    partidasMulti != null ? partidasMulti : 0
            );

        } catch (Exception e) {
            System.out.println("❌ Error al obtener estadísticas: " + e.getMessage());
            return "❌ Error al obtener estadísticas de partidas";
        } finally {
            session.close();
        }
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
