package com.example;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableConfigurationProperties(ReservationsConfig.class)
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}

	@Bean
	public ApplicationRunner init(ReservationsConfig config, ReservationRepository reservations) {
		return args -> Arrays
				.stream(config.getNames().split(","))
				.map(Reservation::new)
				.forEach(reservations::save);
	}
}

@Slf4j
@RestController
@RequestMapping("/reservations")
class ReservationController {

	final ReservationRepository reservations;

	public ReservationController(ReservationRepository reservations) {
		this.reservations = reservations;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	public List<Reservation> list() {
		return reservations.findAll();
	}

	@GetMapping(path = "/{name}", produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<Reservation> findOne(@PathVariable("name") String name) {
		return Optional.ofNullable(reservations.findByName(name))
				.map(ResponseEntity::ok)
				.orElse(notFound().build());
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<?> create(@RequestBody Reservation reservation) {
		log.info("Creating: {}", reservation);
		if (reservations.exists(Example.of(reservation))) {
			return status(CONFLICT).build();
		}
		reservations.save(reservation);
		return created(selfUri(reservation)).build();
	}

	private static URI selfUri(Reservation reservation) {
		return linkTo(methodOn(ReservationController.class).findOne(reservation.name))
				.toUri();
	}

	@DeleteMapping(path = "/{name}")
	@ResponseStatus(NO_CONTENT)
	public void delete(@PathVariable("name") String name) {
		reservations.delete(reservations.findByName(name));
	}
}

interface ReservationRepository extends JpaRepository<Reservation, Long> {

	Reservation findByName(@Param("name") String name);

	@Override
	void delete(Long id);
}

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@Entity
@Table(uniqueConstraints = {
		@UniqueConstraint(columnNames = "name")
})
class Reservation {

	@Id
	@GeneratedValue
	Long id;

	String name;

	Reservation(String name) {
		this.name = name;
	}
}

@Data
@ConfigurationProperties("reservation")
class ReservationsConfig {

	String names;
}