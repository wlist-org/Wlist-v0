pub struct PrintTable {
    headers: Vec<String>,
    body: Vec<Vec<String>>,
    char: Vec<usize>,
    cache: Option<Vec<String>>,
}

pub struct PrintTableCached {
    cache: Vec<String>,
}

impl PrintTable {
    pub fn createFromSlice(headers: Vec<&str>) -> PrintTable {
        let mut s = Vec::new();
        for h in headers {
            s.push(String::from(h));
        }
        PrintTable::create(s)
    }

    pub fn create(headers: Vec<String>) -> PrintTable {
        let mut char = vec![0; headers.len()];
        PrintTable::fill_columns(&mut char, &headers);
        PrintTable { headers, body: Vec::new(), char, cache: None }
    }

    pub fn addBodyFromSlice(self, body: Vec<&str>) -> Self {
        let mut s = Vec::new();
        for b in body {
            s.push(String::from(b));
        }
        self.addBody(s)
    }

    pub fn addBody(mut self, body: Vec<String>) -> Self {
        let len = self.headers.len();
        if len != body.len() {
            panic!("Invalid body length. header: {}, body: {}", len, body.len());
        }
        PrintTable::fill_columns(&mut self.char, &body);
        self.body.push(body);
        self.cache = None;
        self
    }

    fn fill_columns(char: &mut [usize], values: &Vec<String>) {
        let mut i = 0;
        while i < values.len() {
            let count = PrintTable::count(&values[i]);
            let len = (values[i].len() - count) / 2 + count;
            if len > char[i] {
                char[i] = len;
            }
            i += 1;
        }
    }

    fn count(str: &String) -> usize {
        let mut count = 0;
        let s = str.as_bytes();
        for u in s {
            if *u < 127 {
                count += 1;
            }
        }
        count
    }

    fn build_border(&self) -> String {
        let mut builder = Vec::new();
        builder.push(b'+');
        for i in &self.char {
            builder.extend_from_slice(format!("{}+", "-".repeat(*i + 2)).as_bytes());
        }
        String::from_utf8(builder).unwrap()
    }

    fn build_row(&self, row: &Vec<String>) -> String {
        let mut builder = Vec::new();
        builder.push(b'|');
        let mut i = 0;
        while i < row.len() {
            builder.extend_from_slice(format!(" {} ", row[i]).as_bytes());
            let count = PrintTable::count(&row[i]);
            let len = self.char[i] - ((row[i].len() - count) / 2 + count);
            if len > 0 {
                builder.extend_from_slice(format!("{}|", " ".repeat(len)).as_bytes());
            } else {
                builder.push(b'|');
            }
            i += 1;
        }
        String::from_utf8(builder).unwrap()
    }
    
    fn build_cache(&mut self) {
        if let Some(_) = &self.cache {
            return;
        }
        let mut cache = Vec::new();
        cache.push(self.build_border());
        cache.push(self.build_row(&self.headers));
        for body in &self.body {
            cache.push(self.build_row(body));
        }
        self.cache = Some(cache);
    }
    
    pub fn finish(mut self) -> PrintTableCached {
        self.build_cache();
        PrintTableCached { cache: self.cache.unwrap() }
    }

    pub fn print(&mut self) {
        self.build_cache();
        let cache = self.cache.as_ref().unwrap();
        println!("{}", cache[0]);
        println!("{}", cache[1]);
        println!("{}", cache[0]);
        for line in &cache[2..] {
            println!("{}", line);
        }
        println!("{}", cache[0]);
    }
}

impl PrintTableCached {
    pub fn print(&self) {
        println!("{}", self.cache[0]);
        println!("{}", self.cache[1]);
        println!("{}", self.cache[0]);
        for line in &self.cache[2..] {
            println!("{}", line);
        }
        println!("{}", self.cache[0]);
    }
}