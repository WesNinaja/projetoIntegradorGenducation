package com.projetointegrador.service;

import java.nio.charset.Charset;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.projetointegrador.model.UsuarioModel;
import com.projetointegrador.model.dtos.UsuarioCredenciaisDTO;
import com.projetointegrador.model.dtos.UsuarioLoginDTO;
import com.projetointegrador.model.dtos.UsuarioRegistroDTO;
import com.projetointegrador.repository.UsuarioRepository;

@Service
public class UserServices {
	
	@Autowired
	private UsuarioRepository repository;
	private UsuarioCredenciaisDTO credentials;

	private UsuarioModel user;

	private static String encryptPassword(String password) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		return encoder.encode(password);
	}

	private static String generatorToken(String email, String password) {
		String structure = email + ":" + password;
		byte[] structureBase64 = Base64.encodeBase64(structure.getBytes(Charset.forName("US-ASCII")));
		return new String(structureBase64);
	}

	private static String generatorTokenBasic(String email, String password) {
		String structure = email + ":" + password;
		byte[] structureBase64 = Base64.encodeBase64(structure.getBytes(Charset.forName("US-ASCII")));
		return "Basic " + new String(structureBase64);
	}

	public ResponseEntity<UsuarioModel> registerUser(@Valid UsuarioRegistroDTO newUser) {
		Optional<UsuarioModel> optional = repository.findByEmail(newUser.getEmail());
		if (optional.isPresent()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Usuario ja existente, cadastre com outro email!");
		} else {
			user = new UsuarioModel();
			user.setNomeCompleto(newUser.getNomeCompleto());
			user.setEmail(newUser.getEmail());
			user.setToken(generatorToken(newUser.getEmail(), newUser.getSenha()));
			user.setSenha(encryptPassword(newUser.getSenha()));
			user.setTipo(newUser.getTipo());
			user.setFoto(newUser.getFoto());
			return ResponseEntity.status(201).body(repository.save(user));
		}
	}
	
	public ResponseEntity<UsuarioModel> atualizar(@Valid UsuarioModel newUser){
		Optional<UsuarioModel> optional = repository.findByEmail(newUser.getEmail());
		
		if (optional.isPresent()) {
			
			optional.get().setToken(generatorToken(optional.get().getEmail(), newUser.getSenha()));
			optional.get().setNomeCompleto(newUser.getNomeCompleto());
			optional.get().setSenha(encryptPassword(newUser.getSenha()));
			optional.get().setTipo(newUser.getTipo());
			optional.get().setFoto(newUser.getFoto());
			
			return ResponseEntity.status(201).body(repository.save(optional.get()));
		}
			else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao atualizar!");
			
			}
		}

	
	public ResponseEntity<UsuarioCredenciaisDTO> getCredentials(@Valid UsuarioLoginDTO userDto) {
		return repository.findByEmail(userDto.getEmail()).map(resp -> {
			BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
			
			if (encoder.matches(userDto.getSenha(), resp.getSenha())) {
				
				credentials = new UsuarioCredenciaisDTO();
				credentials.setEmail(resp.getEmail());
				credentials.setNomeCompleto(resp.getNomeCompleto());
				credentials.setTipo(resp.getTipo());
				credentials.setFoto(resp.getFoto());
				credentials.setToken(resp.getToken());
				credentials.setTokenBasic(generatorTokenBasic(userDto.getEmail(), userDto.getSenha()));
				
				return ResponseEntity.status(200).body(credentials);
			} else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha incorreta!");
			}
			
		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email incorreto!"));
		
	}
}