package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "API de planos")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // GET /api/v1/blueprints
    @Operation(summary = "Consultar todos los bluprints")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta exitosa")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(200, "consulta exitosa", services.getAllBlueprints()));
    }

    // GET /api/v1/blueprints/{author}
    @Operation(summary = "consultar los blueprints de  autor")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El autor no tiene blueprints")
    })
    @GetMapping("/{author}")
    public ResponseEntity<?> byAuthor(@PathVariable String author) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(200, "consulta exitosa", services.getBlueprintsByAuthor(author)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    // GET /api/v1/blueprints/{author}/{bpname}
    @Operation(summary = "consultar blueprint por autor y nombre")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "consulta exitosa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "el blueprint no existe")
    })

    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<?> byAuthorAndName(@PathVariable String author, @PathVariable String bpname) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(200, "execute ok", services.getBlueprint(author, bpname)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    // POST /api/v1/blueprints
    @Operation(summary = "Crear un blueprint")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "se creo el blueprint"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "los datos son invalidos o blueprint ya existente")
    })

    @PostMapping
    public ResponseEntity<?> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "blueprint creado", bp));
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(400, e.getMessage(), null));
        }
    }

    // PUT /api/v1/blueprints/{author}/{bpname}/points
    @Operation(summary = "Agregar un punto a un blueprint")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "punto agregado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "dato invalidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "el blueprint no existe")
    })
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<?> addPoint(@PathVariable String author, @PathVariable String bpname,
                                      @RequestBody Point p) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(202, "punto agregado", p));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    //400 falla el valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(400, "datos invalidos", null));
    }
    //400 json esta mal o vacio
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleVAlidation(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(400, "No es valido el cuerpo de la peticion", null));
    }


    public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            @Valid java.util.List<Point> points
    ) { }
}
