package mejai.mejaigg.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import mejai.mejaigg.domain.Game;
import mejai.mejaigg.domain.MatchParticipant;
import mejai.mejaigg.domain.Rank;
import mejai.mejaigg.domain.User;
import mejai.mejaigg.domain.UserGameStat;
import mejai.mejaigg.dto.response.UserProfileDto;
import mejai.mejaigg.dto.response.UserStreakDto;
import mejai.mejaigg.dto.riot.AccountDto;
import mejai.mejaigg.dto.riot.RankDto;
import mejai.mejaigg.dto.riot.SummonerDto;
import mejai.mejaigg.dto.riot.match.MatchDto;
import mejai.mejaigg.dto.riot.match.ParticipantDto;
import mejai.mejaigg.mapper.GameMapper;
import mejai.mejaigg.mapper.RankMapper;
import mejai.mejaigg.mapper.UserMapper;
import mejai.mejaigg.repository.GameRepository;
import mejai.mejaigg.repository.RankRepository;
import mejai.mejaigg.repository.UserGameStatRepository;
import mejai.mejaigg.repository.UserRepository;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

	private final ApiService apiService;
	private final UserRepository userRepository;
	private final GameRepository gameRepository;
	private final UserGameStatRepository userGameStatRepository;
	private final RankRepository rankRepository;

	@Value("${season:14}") // 기본값으로 14을 사용
	private int season;
	@Value("${resourceURL:https://ddragon.leagueoflegends.com/cdn/11.16.1/img/profileicon/}")
	private String requestUrl;

	//TODO : api 콜을 실패하는 경우 고려해야함
	//TODO : 비동기 레포지토리 방식 적용
	//TODO : 시즌 바꼈을때 추가하는 로직 필요하다.
	//처음으로 요청이 들어왔을 때 호출되는 서비스
	@Transactional(readOnly = false)
	public String setUserProfile(String name, String tag) { //처음 콜 할 때 세팅 되는 함수
		Mono<AccountDto> account = apiService.getAccountByNameAndTag(name, tag);
		AccountDto accountDto = account.block();
		Mono<SummonerDto> summoner = apiService.getSummonerByPuuid(accountDto.getPuuid());
		SummonerDto summonerDto = summoner.block();
		User user = UserMapper.INSTANCE.toUserEntity(accountDto, summonerDto);

		Mono<Set<RankDto>> rankBySummonerId = apiService.getRankBySummonerId(summonerDto.getId());
		RankDto rankDto = rankBySummonerId.block()
			.stream()
			.findFirst()
			.orElse(null);
		Rank rank = new Rank();

		if (rankDto != null)  //랭크가 없는 경우에는 배열이 비었다. (언랭 유저)
			rank = RankMapper.INSTANCE.toRankEntity(rankDto);
		else
			rank.setUnRanked();
		user.setRank(rank);
		rankRepository.save(rank);
		userRepository.save(user);
		return user.getPuuid();
	}

	@Transactional(readOnly = false)
	public String updateUserProfile(User user) {
		Mono<SummonerDto> summoner = apiService.getSummonerByPuuid(user.getPuuid());
		SummonerDto summonerDto = summoner.block();
		Mono<Set<RankDto>> rankBySummonerId = apiService.getRankBySummonerId(summonerDto.getId());
		RankDto rankDto = rankBySummonerId.block()
			.stream()
			.findFirst()
			.orElse(null);
		user.updateBySummonerDto(summonerDto);
		user.getRank().updateByRankDto(rankDto);
		return user.getPuuid();
	}

	public Set<UserStreakDto> getUserStreak(AccountDto accountDto, User user) {
		try {
			Mono<String[]> matchHistoryByPuuid = apiService.getMatchHistoryByPuuid(accountDto.getPuuid(),
				20); //임시로 20개임
			String[] matchHistory = matchHistoryByPuuid.block();
			for (String matchId : matchHistory) {
				MatchParticipant matchParticipant = new MatchParticipant();
				Mono<MatchDto> gameInfo = apiService.getMatchDtoByMatchId(matchId);
				MatchDto matchDto = gameInfo.block();

				Game game = GameMapper.INSTANCE.toGameEntity(matchDto.getInfo(), matchId);
				user.addMatchParticipant(matchParticipant);
				game.addMatchParticipant(matchParticipant);
				ParticipantDto[] participants = matchDto.getInfo().getParticipants();
				List<UserGameStat> userGameStats = new ArrayList<>();
				for (ParticipantDto participant : participants) {
					UserGameStat userGameStat = new UserGameStat();
					userGameStat.setByParticipantDto(participant);
					userGameStats.add(userGameStat);
				}
				game.setGameStats(userGameStats);
				gameRepository.save(game);
			}
			userRepository.save(user);
		} catch (Exception e) {
			e.printStackTrace();

		}
		return null;
	}

	@Transactional
	public UserProfileDto getUserProfileByNameTag(String name, String tag) {
		Optional<User> userOptional = userRepository.findBySummonerNameAndTagLineAllIgnoreCase(name, tag);
		if (userOptional.isEmpty()) {
			String puuid = setUserProfile(name, tag);
			if (puuid == null) {
				throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "summoner not found"
				);
			}
			System.out.println("ERROR :::: puuid = " + puuid);
			userOptional = userRepository.findById(puuid);
		} else {
			updateUserProfile(userOptional.get());
		}
		UserProfileDto userProfileDto = new UserProfileDto();
		if (userOptional.isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND, "summoner not found"
			);
		}
		User user = userOptional.get();
		userProfileDto.setByUser(user, requestUrl);
		return userProfileDto;
	}
}
