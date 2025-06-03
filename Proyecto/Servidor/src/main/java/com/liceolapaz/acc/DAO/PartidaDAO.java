package com.liceolapaz.acc.DAO;

import com.liceolapaz.acc.entidades.Partida;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class PartidaDAO {

    // Registro de servicios estándar de Hibernate configurado desde hibernate.cfg.xml
    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    // Fábrica de sesiones de Hibernate para gestionar conexiones a la base de datos
    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    /**
     * Registra una nueva partida en la base de datos con categoría, estado y tipo específicos
     * @param categoria Categoría de la partida (se convierte a minúsculas)
     * @param completada Estado de finalización de la partida
     * @param tipoPartida Tipo de partida (se convierte a mayúsculas)
     * @return La partida creada o null si hay error
     */
    public static Partida registrarPartida(String categoria, boolean completada, String tipoPartida) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            Partida partida = new Partida();
            partida.setCategoria(categoria.toLowerCase());
            partida.setCompletada(completada);
            partida.setTipoPartida(tipoPartida.toUpperCase());
            partida.setTotalPreguntas(10); // Número fijo de preguntas por partida

            session.persist(partida);
            tx.commit();

            System.out.println("✅ Partida registrada - ID: " + partida.getId() +
                    ", Categoría: " + categoria +
                    ", Tipo: " + tipoPartida +
                    ", Completada: " + completada);

            return partida;
        } catch (Exception e) {
            // Revertir transacción en caso de error
            if (tx != null) tx.rollback();
            System.out.println("❌ Error al registrar la partida: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    /**
     * Registra una nueva partida individual (tipo SOLO por defecto)
     * @param categoria Categoría de la partida
     * @param completada Estado de finalización de la partida
     * @return La partida creada o null si hay error
     */
    public static Partida registrarPartida(String categoria, boolean completada) {
        return registrarPartida(categoria, completada, "SOLO");
    }

    /**
     * Marca una partida existente como completada
     * @param partidaId ID de la partida a marcar como completada
     */
    public static void marcarPartidaComoCompletada(int partidaId) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar la partida por ID
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

    /**
     * Establece la duración total de una partida en segundos
     * @param partidaId ID de la partida
     * @param duracionSegundos Duración de la partida en segundos
     */
    public static void establecerDuracionPartida(int partidaId, long duracionSegundos) {
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Buscar la partida por ID
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

    /**
     * Obtiene una partida específica por su ID
     * @param partidaId ID de la partida a buscar
     * @return La partida encontrada o null si no existe
     */
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

    /**
     * Cuenta el número total de partidas jugadas en una categoría específica
     * @param categoria Categoría a consultar
     * @return Número de partidas en la categoría
     */
    public static long contarPartidasPorCategoria(String categoria) {
        Session session = sf.openSession();
        try {
            // Consulta HQL para contar partidas por categoría
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

    /**
     * Cuenta el número total de partidas completadas en el sistema
     * @return Número de partidas completadas
     */
    public static long contarPartidasCompletadas() {
        Session session = sf.openSession();
        try {
            // Consulta HQL para contar partidas completadas
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

    /**
     * Obtiene estadísticas generales de todas las partidas del sistema
     * @return String formateado con estadísticas completas de partidas
     */
    public static String obtenerEstadisticasPartidas() {
        Session session = sf.openSession();
        try {
            // Obtener total de partidas
            Long totalPartidas = (Long) session.createQuery(
                    "SELECT COUNT(p) FROM Partida p").uniqueResult();

            // Obtener partidas completadas
            Long partidasCompletadas = (Long) session.createQuery(
                    "SELECT COUNT(p) FROM Partida p WHERE p.completada = true").uniqueResult();

            // Obtener partidas individuales
            Long partidasSolo = (Long) session.createQuery(
                    "SELECT COUNT(p) FROM Partida p WHERE p.tipoPartida = 'SOLO'").uniqueResult();

            // Obtener partidas multijugador
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