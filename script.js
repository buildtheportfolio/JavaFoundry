const API = 'https://api.github.com/repos/buildtheportfolio/JavaFoundry/contents/projects';
const projectsEl = document.getElementById('projects');
const statusEl = document.getElementById('status');
const searchEl = document.getElementById('search');
const countEl = document.getElementById('count');
let projects = [];

function titleFromSlug(slug) {
  return slug.replace(/[-_]+/g, ' ').replace(/\b\w/g, char => char.toUpperCase());
}

async function isValidProject(entry) {
  if (entry.type !== 'dir' || entry.name === '_template') return false;

  const response = await fetch(`${API}/${encodeURIComponent(entry.name)}`, {
    headers: { Accept: 'application/vnd.github+json' }
  });
  if (!response.ok) return false;

  const files = await response.json();
  return files.some(file => file.type === 'file' && file.name === 'pom.xml');
}

function render() {
  const query = searchEl.value.trim().toLowerCase();
  const visible = projects.filter(project => project.name.toLowerCase().includes(query));
  countEl.textContent = `${visible.length} project${visible.length === 1 ? '' : 's'}`;
  projectsEl.innerHTML = visible.length
    ? visible.map(project => `<a class="card" href="projects/${encodeURIComponent(project.slug)}/"><h2>${project.name}</h2><p>Java and Spring Boot project.</p><span class="open">Open project →</span></a>`).join('')
    : '<div class="empty">No projects match your search.</div>';
  statusEl.hidden = true;
}

async function loadProjects() {
  try {
    const response = await fetch(API, { headers: { Accept: 'application/vnd.github+json' } });
    if (!response.ok) throw new Error(`GitHub API returned ${response.status}`);

    const entries = await response.json();
    const validEntries = await Promise.all(entries.map(async entry => ({
      entry,
      valid: await isValidProject(entry)
    })));

    projects = validEntries
      .filter(({ valid }) => valid)
      .map(({ entry }) => ({ slug: entry.name, name: titleFromSlug(entry.name) }))
      .sort((a, b) => a.name.localeCompare(b.name));

    render();
  } catch (error) {
    projectsEl.innerHTML = '<div class="empty">Projects could not be loaded. Refresh and try again.</div>';
    countEl.textContent = '';
    statusEl.hidden = true;
    console.error(error);
  }
}

searchEl.addEventListener('input', render);
loadProjects();
