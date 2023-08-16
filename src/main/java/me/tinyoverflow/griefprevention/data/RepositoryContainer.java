package me.tinyoverflow.griefprevention.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.tinyoverflow.griefprevention.data.repositories.UserRepository;

public class RepositoryContainer
{

    @Getter
    private final UserRepository userRepository;

    private RepositoryContainer(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    public static RepositoryContainerBuilder builder()
    {
        return new RepositoryContainerBuilder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RepositoryContainerBuilder
    {
        private UserRepository userRepository;

        public RepositoryContainerBuilder withUserRepository(UserRepository userRepository)
        {
            this.userRepository = userRepository;
            return this;
        }

        public RepositoryContainer build()
        {
            return new RepositoryContainer(
                    userRepository
            );
        }
    }
}
