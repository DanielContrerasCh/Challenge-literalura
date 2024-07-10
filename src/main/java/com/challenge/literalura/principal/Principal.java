package com.challenge.literalura.principal;

import com.challenge.literalura.model.*;
import com.challenge.literalura.repository.AutorRepository;
import com.challenge.literalura.repository.LibroRepository;
import com.challenge.literalura.service.ConsumoAPI;
import com.challenge.literalura.service.ConvierteDatos;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Principal {

    private String URL_BASE = "https://gutendex.com/books/";
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private LibroRepository libroRepository;
    private AutorRepository autorRepository;

    public Principal(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void menu() {

        var option = -1;
        while (option != 0) {
            var menu = """
                    =======================================
                    Bienvenido al sistema Literalura
                    =======================================
                    Seleccione una opción
                    
                    1 - Buscar libro por Título
                    2 - Mostrar libros guardados
                    3 - Mostrar Autores guardados
                    4 - Buscar autores vivos en un determinado año
                    5 - Mostrar libros por idioma
                    6 - top 10 libros más descargados guardados
                    7 - top 10 libros más descargados de la API
                    8 - Buscar autor por nombre
                                   
                    0 - Exit
                    """;
            System.out.println(menu);
            try{
                option = teclado.nextInt();
            } catch (Exception e){
                System.out.println("Entrada no válida, por favor ingrese un número");
                teclado.nextLine();
                continue;
            }

            teclado.nextLine();

            switch (option) {
                case 0:
                    System.out.println("Gracias por usar el sistema Literalura");
                    break;
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    mostarLibrosguardados();
                    break;
                case 3:
                    mostrarAutoresGuardados();
                    break;
                case 4:
                    mostrarAutoresPorAnio();
                    break;
                case 5:
                    mostrarLibrosPorIdioma();
                    break;
                case 6:
                    mostrarTop10LibrosGuardados();
                    break;
                case 7:
                    mostrarTop10LibrosAPI();
                    break;
                case 8:
                    buscarAutorPorNombre();
                    break;
                default:
                    System.out.println("Opción no valida");
            }
        }
    }

    private void guardarLibro(DatosLibros libroBuscado) {
        Libro libro = new Libro(libroBuscado);

        Libro libroGuardado = libroRepository.findByTitulo(libro.getTitulo());
        if(libroGuardado != null){
            System.out.println("El libro ya se encuentra guardado");
            return;
        }

        for (DatosAutor datosAutor : libroBuscado.autores()) {
            Autor autor = autorRepository.findByNombreContainsIgnoreCase(datosAutor.nombre());
            if (autor == null) {
                autor = new Autor(datosAutor);
            }
            autor.setLibros(libro);
            libro.setAutores(autor);
            autorRepository.save(autor);
        }

        System.out.println(libro);
        libroRepository.save(libro);
    }

    private void buscarLibroPorTitulo() {
        System.out.println("Ingrese el nombre del libro que desea buscar");
        var tituloLibro = teclado.nextLine();
        var respuesta = consumoAPI.obtenerDatos(URL_BASE + "?search=" +
                tituloLibro.replace(" ", "+"));
        var busqueda = conversor.convertirDatos(respuesta, Datos.class);
        Optional<DatosLibros> libroBuscado = busqueda.resultados().stream()
                .filter(l -> l.titulo().toUpperCase().contains(tituloLibro.toUpperCase()))
                .findFirst();
        if(libroBuscado.isPresent()){
            guardarLibro(libroBuscado.get());
        }else {
            System.out.println("Libro no encontrado");
        }
    }

    private void mostarLibrosguardados() {
        List<Libro> libros = libroRepository.findAll();
        libros.stream()
                .sorted(Comparator.comparing(Libro::getNumeroDeDescargas).reversed())
                .forEach(System.out::println);
    }

    private void mostrarAutoresGuardados() {
        List<Autor> autores = autorRepository.findAll();
        autores.stream()
                .sorted(Comparator.comparing(Autor::getNombre))
                .forEach(System.out::println);
    }

    private void mostrarAutoresPorAnio() {
        System.out.println("Ingrese el año que desea buscar");
        try{
            var anio = teclado.nextInt();
            var autores = autorRepository.findByFechaDeNacimientoLessThanEqualAndFechaDeFallecimientoGreaterThanEqual(anio, anio);
            autores.stream()
                    .sorted(Comparator.comparing(Autor::getNombre))
                    .forEach(System.out::println);
        } catch (Exception e){
            System.out.println("Entrada no válida, por favor ingrese un número");
            teclado.nextLine();
        }
    }

    private void mostrarLibrosPorIdioma() {
        System.out.println("Ingrese el idioma que desea buscar");
        var listaIdiomas = """
                es- Español
                en- Ingles
                fr- Frances
                pt- Portugues
                """;
        System.out.println(listaIdiomas);
        var idioma = teclado.nextLine();
        var libros = libroRepository.findByIdiomas(idioma);
        libros.stream()
                .sorted(Comparator.comparing(Libro::getTitulo))
                .forEach(System.out::println);
    }

    private void mostrarTop10LibrosGuardados() {
        List<Libro> libros = libroRepository.findTop10ByOrderByNumeroDeDescargasDesc();
        libros.stream()
                .forEach(System.out::println);
    }

    private void mostrarTop10LibrosAPI() {
        var respuesta = consumoAPI.obtenerDatos(URL_BASE + "?sort");
        var busqueda = conversor.convertirDatos(respuesta, Datos.class);
        List<DatosLibros> librosBuscados = busqueda.resultados();
        List<Libro> libros = librosBuscados.stream()
                .sorted(Comparator.comparing(DatosLibros::numeroDeDescargas).reversed())
                .limit(10)
                .map(datosLibro -> {
                    Libro libro = new Libro(datosLibro);
                    for (DatosAutor datosAutor : datosLibro.autores()) {
                        Autor autor = new Autor(datosAutor);
                        libro.setAutores(autor);
                    }
                    return libro;
                })
                .toList();

        libros.forEach(System.out::println);
    }

    private void buscarAutorPorNombre() {
        System.out.println("Ingrese el nombre del autor que desea buscar");
        var nombreAutor = teclado.nextLine();
        var autor = autorRepository.buscarPorNombre(nombreAutor);
        if(autor.isEmpty()){
            System.out.println("Autor no encontrado");
        }else {
            System.out.println(autor);
        }
    }

}
